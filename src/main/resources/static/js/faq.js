document.addEventListener('DOMContentLoaded', function () {

    const pageMode = document.body.dataset.faqMode === 'admin' ? 'admin' : 'customer';
    const adminEmail = document.body.dataset.adminEmail || '';

    const searchInput = document.getElementById('faqSearchInput');
    const categoryGrid = document.getElementById('faqCategoryGrid');
    const accordion = document.getElementById('faqAccordion');
    const noResults = document.getElementById('faqNoResults');
    const visibleCount = document.getElementById('faqVisibleCount');
    const loadingState = document.getElementById('faqLoadingState');
    const loadErrorState = document.getElementById('faqLoadError');
    const addButton = document.getElementById('faqAddButton');

    // Modal elements (admin only — these won't exist on the customer page)
    const modalOverlay = document.getElementById('faqModalOverlay');
    const modalTitle = document.getElementById('faqModalTitle');
    const modalForm = document.getElementById('faqModalForm');
    const modalError = document.getElementById('faqModalError');
    const formId = document.getElementById('faqFormId');
    const formTitle = document.getElementById('faqFormTitle');
    const formCategory = document.getElementById('faqFormCategory');
    const formBody = document.getElementById('faqFormBody');
    const formDeleteBtn = document.getElementById('faqFormDelete');
    const formCancelBtn = document.getElementById('faqFormCancel');
    const modalCloseBtn = document.getElementById('faqModalClose');
    const categoryOptionsList = document.getElementById('faqCategoryOptions');

    let articles = [];       // KnowledgeSummary-shaped objects from the list endpoint
    let bodyCache = {};      // id -> { body, category, updatedAt, version } once lazily loaded
    let activeCategory = 'all';

    loadArticles();
    if (pageMode === 'admin') {
        loadCategoryOptions();
    }

    // --- Load the article list ------------------------------------------
    function loadArticles() {
        setLoading(true);

        const url = pageMode === 'admin' ? '/api/knowledge/admin?size=200' : '/api/knowledge?size=200';

        fetch(url, { credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('Request failed with status ' + res.status);
                }
                return res.json();
            })
            .then(function (page) {
                articles = page.content || [];
                setLoading(false);
                showLoadError(false);
                renderCategoryChips();
                renderArticles();
            })
            .catch(function () {
                setLoading(false);
                showLoadError(true);
            });
    }

    function loadCategoryOptions() {
        fetch('/api/categories', { credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok) throw new Error('Failed to load categories');
                return res.json();
            })
            .then(function (categories) {
                if (!categoryOptionsList) return;
                categoryOptionsList.innerHTML = '';
                categories.forEach(function (cat) {
                    const option = document.createElement('option');
                    option.value = cat.name;
                    categoryOptionsList.appendChild(option);
                });
            })
            .catch(function () {
                // Non-critical: the admin can still type a free-text category
                // name even if this autocomplete list fails to load.
            });
    }

    function setLoading(isLoading) {
        if (loadingState) loadingState.hidden = !isLoading;
        if (isLoading) {
            if (accordion) accordion.hidden = true;
            if (categoryGrid) categoryGrid.hidden = true;
        } else {
            if (accordion) accordion.hidden = false;
            if (categoryGrid) categoryGrid.hidden = false;
        }
    }

    function showLoadError(hasError) {
        if (loadErrorState) loadErrorState.hidden = !hasError;
    }

    // --- Category chips ---------------------------------------------------
    function renderCategoryChips() {
        if (!categoryGrid) return;

        const counts = {};
        articles.forEach(function (a) {
            const name = a.category ? a.category.name : 'Uncategorized';
            counts[name] = (counts[name] || 0) + 1;
        });

        const names = Object.keys(counts).sort(function (a, b) {
            return a.localeCompare(b);
        });

        let html = '';
        html += categoryButtonHtml('all', 'All Topics', articles.length, true);
        names.forEach(function (name) {
            html += categoryButtonHtml(name, name, counts[name], false);
        });

        categoryGrid.innerHTML = html;

        categoryGrid.querySelectorAll('.faq-category-card').forEach(function (button) {
            button.addEventListener('click', function () {
                categoryGrid.querySelectorAll('.faq-category-card').forEach(function (b) {
                    b.classList.remove('faq-category-card--active');
                });
                button.classList.add('faq-category-card--active');
                activeCategory = button.dataset.category;
                applyFilters();
            });
        });
    }

    function categoryButtonHtml(key, label, count, isActive) {
        const activeClass = isActive ? ' faq-category-card--active' : '';
        const articleWord = count === 1 ? 'article' : 'articles';

        return (
            '<button type="button" class="faq-category-card' + activeClass + '" data-category="' + escapeHtml(key) + '">' +
                '<span class="faq-category-title">' + escapeHtml(label) + '</span>' +
                '<span class="faq-category-count">' + count + ' ' + articleWord + '</span>' +
            '</button>'
        );
    }

    // --- Accordion list -----------------------------------------------
    function renderArticles() {
        if (!accordion) return;

        if (articles.length === 0) {
            accordion.innerHTML = '';
            if (visibleCount) visibleCount.textContent = '0 articles';
            if (noResults) noResults.hidden = false;
            return;
        }

        accordion.innerHTML = articles.map(renderArticleItem).join('');
        applyFilters();
    }

    function renderArticleItem(article) {
        const categoryName = article.category ? article.category.name : 'Uncategorized';
        const statusBadge = pageMode === 'admin' ? statusBadgeHtml(article.status) : '';
        const actionsHtml = pageMode === 'admin'
            ? '<div class="faq-item-actions">' +
                '<button type="button" class="faq-item-action" data-action="edit" aria-label="Edit article">✎</button>' +
                '<button type="button" class="faq-item-action faq-item-action--delete" data-action="delete" aria-label="Delete article">🗑</button>' +
              '</div>'
            : '';

        return (
            '<div class="faq-item" data-id="' + article.id + '" data-category="' + escapeHtml(categoryName) + '">' +
                '<div class="faq-item-header">' +
                    '<button type="button" class="faq-question" aria-expanded="false">' +
                        '<span>' + escapeHtml(article.title) + statusBadge + '</span>' +
                    '</button>' +
                    actionsHtml +
                    '<span class="faq-chevron">⌄</span>' +
                '</div>' +
                '<div class="faq-answer" data-loaded="false">' +
                    '<p class="faq-answer-placeholder">Loading answer…</p>' +
                '</div>' +
            '</div>'
        );
    }

    function statusBadgeHtml(status) {
        if (!status || status === 'PUBLISHED') {
            return '<span class="faq-status-badge faq-status-published">Published</span>';
        }
        if (status === 'DRAFT') {
            return '<span class="faq-status-badge faq-status-draft">Draft</span>';
        }
        return '<span class="faq-status-badge faq-status-other">' + escapeHtml(status) + '</span>';
    }

    // --- Accordion toggle + lazy-load the answer body on first expand ----
    if (accordion) {
        accordion.addEventListener('click', function (event) {
            const actionButton = event.target.closest('.faq-item-action');
            if (actionButton) {
                handleItemAction(actionButton);
                return;
            }

            const header = event.target.closest('.faq-item-header');
            if (!header) return;

            const item = header.closest('.faq-item');
            const isOpen = item.classList.contains('is-open');

            item.classList.toggle('is-open', !isOpen);
            item.querySelector('.faq-question').setAttribute('aria-expanded', String(!isOpen));

            if (!isOpen) {
                loadAnswerBody(item);
            }
        });
    }

    function loadAnswerBody(item) {
        const id = item.dataset.id;
        const answerEl = item.querySelector('.faq-answer');

        if (answerEl.dataset.loaded === 'true') {
            return; // already fetched this session
        }

        const detailUrl = pageMode === 'admin' ? '/api/knowledge/admin/' + id : '/api/knowledge/' + id;

        fetch(detailUrl, { credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok) throw new Error('Failed to load article');
                return res.json();
            })
            .then(function (detail) {
                bodyCache[id] = {
                    body: detail.body,
                    category: detail.category ? detail.category.name : '',
                    updatedAt: detail.updatedAt,
                    version: detail.version
                };

                answerEl.innerHTML =
                    '<p>' + escapeHtml(detail.body) + '</p>' +
                    '<span class="faq-updated-tag">Last updated: ' + formatDate(detail.updatedAt) + '</span>';
                answerEl.dataset.loaded = 'true';

                // A body we just fetched should count toward search matches too.
                applyFilters();
            })
            .catch(function () {
                answerEl.innerHTML = '<p class="faq-answer-placeholder">Couldn\'t load this answer. Try again.</p>';
            });
    }

    // --- Search + category filter -----------------------------------
    if (searchInput) {
        searchInput.addEventListener('input', applyFilters);
    }

    function applyFilters() {
        if (!accordion) return;

        const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
        const items = accordion.querySelectorAll('.faq-item');
        let matches = 0;

        items.forEach(function (item) {
            const id = item.dataset.id;
            const category = item.dataset.category;
            const cached = bodyCache[id];

            const searchableText = [
                item.querySelector('.faq-question span').textContent,
                category,
                cached ? cached.body : ''
            ].join(' ').toLowerCase();

            const matchesCategory = activeCategory === 'all' || category === activeCategory;
            const matchesQuery = query === '' || searchableText.includes(query);
            const isVisible = matchesCategory && matchesQuery;

            item.hidden = !isVisible;
            if (isVisible) matches += 1;
        });

        if (noResults) noResults.hidden = matches !== 0;
        if (visibleCount) visibleCount.textContent = matches + (matches === 1 ? ' article' : ' articles');
    }

    // --- Admin: add / edit / delete ---------------------------------
    if (addButton) {
        addButton.addEventListener('click', function () {
            openModal('create');
        });
    }

    function handleItemAction(button) {
        const item = button.closest('.faq-item');
        const id = item.dataset.id;
        const action = button.dataset.action;

        if (action === 'edit') {
            openEditModal(id);
        } else if (action === 'delete') {
            deleteArticle(id);
        }
    }

    function openModal(formMode, article) {
        if (!modalOverlay) return;

        modalError.hidden = true;
        modalError.textContent = '';

        if (formMode === 'create') {
            modalTitle.textContent = 'Add Article';
            formId.value = '';
            formTitle.value = '';
            formCategory.value = '';
            formBody.value = '';
            formDeleteBtn.hidden = true;
        } else {
            modalTitle.textContent = 'Edit Article';
            formId.value = article.id;
            formTitle.value = article.title;
            formCategory.value = article.category ? article.category.name : '';
            formBody.value = article.body;
            formDeleteBtn.hidden = false;
        }

        modalOverlay.hidden = false;
        formTitle.focus();
    }

    function closeModal() {
        if (modalOverlay) modalOverlay.hidden = true;
    }

    function openEditModal(id) {
        const detailUrl = pageMode === 'admin' ? '/api/knowledge/admin/' + id : '/api/knowledge/' + id;

        fetch(detailUrl, { credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok) throw new Error('Failed to load article');
                return res.json();
            })
            .then(function (detail) {
                openModal('edit', detail);
            })
            .catch(function () {
                window.alert('Could not load this article for editing. Please try again.');
            });
    }

    function deleteArticle(id) {
        const confirmed = window.confirm('Delete this article? This cannot be undone.');
        if (!confirmed) return;

        fetch('/api/knowledge/' + id, { method: 'DELETE', credentials: 'same-origin' })
            .then(function (res) {
                if (!res.ok && res.status !== 204) throw new Error('Delete failed');
                delete bodyCache[id];
                loadArticles();
            })
            .catch(function () {
                window.alert('Could not delete this article. Please try again.');
            });
    }

    if (modalCloseBtn) modalCloseBtn.addEventListener('click', closeModal);
    if (formCancelBtn) formCancelBtn.addEventListener('click', closeModal);
    if (modalOverlay) {
        modalOverlay.addEventListener('click', function (event) {
            if (event.target === modalOverlay) closeModal();
        });
    }

    if (formDeleteBtn) {
        formDeleteBtn.addEventListener('click', function () {
            const id = formId.value;
            if (!id) return;
            closeModal();
            deleteArticle(id);
        });
    }

    if (modalForm) {
        modalForm.addEventListener('submit', function (event) {
            event.preventDefault();

            const id = formId.value;
            const payload = {
                title: formTitle.value.trim(),
                summary: null,
                body: formBody.value.trim(),
                category: formCategory.value.trim(),
                tags: [],
                relatedArticleIds: [],
                author: adminEmail || null,
                status: 'PUBLISHED'
            };

            const isEdit = Boolean(id);
            const url = isEdit ? '/api/knowledge/' + id : '/api/knowledge';
            const method = isEdit ? 'PUT' : 'POST';

            fetch(url, {
                method: method,
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            })
                .then(function (res) {
                    if (res.ok) return res.json();
                    return res.json().then(function (err) {
                        throw err;
                    });
                })
                .then(function () {
                    closeModal();
                    loadArticles();
                })
                .catch(function (err) {
                    modalError.hidden = false;
                    modalError.textContent = formatApiError(err);
                });
        });
    }

    function formatApiError(err) {
        if (!err) return 'Something went wrong. Please try again.';
        const parts = [];
        if (err.message) parts.push(err.message);
        if (Array.isArray(err.fieldErrors)) {
            err.fieldErrors.forEach(function (fe) {
                parts.push(fe.field + ': ' + fe.message);
            });
        }
        return parts.length ? parts.join(' — ') : 'Something went wrong. Please try again.';
    }

    // --- Small helpers -----------------------------------------------
    function formatDate(value) {
        if (!value) return 'unknown';
        const date = Array.isArray(value)
            ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0)
            : new Date(value);

        if (isNaN(date.getTime())) return 'unknown';

        return date.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    }

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }
});