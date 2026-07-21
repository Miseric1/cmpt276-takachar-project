document.addEventListener('DOMContentLoaded', function () {

    const searchInput = document.getElementById('faqSearchInput');
    const categoryButtons = document.querySelectorAll('.faq-category-card');
    const faqItems = document.querySelectorAll('.faq-item');
    const noResults = document.getElementById('faqNoResults');
    const visibleCount = document.getElementById('faqVisibleCount');

    let activeCategory = 'all';

    // --- Accordion toggle ---------------------------------------------
    faqItems.forEach(function (item) {
        const question = item.querySelector('.faq-question');

        question.addEventListener('click', function () {
            const isOpen = item.classList.contains('is-open');

            item.classList.toggle('is-open', !isOpen);
            question.setAttribute('aria-expanded', String(!isOpen));
        });
    });

    // --- Category filter -------------------------------------------------
    categoryButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            categoryButtons.forEach(function (b) {
                b.classList.remove('faq-category-card--active');
            });
            button.classList.add('faq-category-card--active');

            activeCategory = button.dataset.category;
            applyFilters();
        });
    });

    // --- Search filter -----------------------------------------------
    if (searchInput) {
        searchInput.addEventListener('input', applyFilters);
    }

    // --- Combined filter logic -----------------------------------------
    function applyFilters() {
        const query = searchInput ? searchInput.value.trim().toLowerCase() : '';
        let matches = 0;

        faqItems.forEach(function (item) {
            const category = item.dataset.category;
            const text = item.textContent.toLowerCase();

            const matchesCategory = activeCategory === 'all' || category === activeCategory;
            const matchesQuery = query === '' || text.includes(query);
            const isVisible = matchesCategory && matchesQuery;

            item.hidden = !isVisible;
            if (isVisible) {
                matches += 1;
            }
        });

        if (noResults) {
            noResults.hidden = matches !== 0;
        }

        if (visibleCount) {
            visibleCount.textContent = matches + (matches === 1 ? ' article' : ' articles');
        }
    }
});