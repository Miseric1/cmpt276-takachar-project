(function () {
    'use strict';
    
    // ── Constants ────────────────────────────────────────────────
    const TREE_MODE = document.body.dataset.treeMode || 'admin';
    const IS_CUSTOMER = TREE_MODE === 'customer';
    const API         = '/api';
    const MAX_OPTS    = 10;
    const LETTERS     = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const DEFAULT_CATEGORIES = [
        'Modbus / HMI',
        'Azure / Wi-Fi',
        'SD card / logging',
        'PWM / Blowers'
    ];
    // ── State ────────────────────────────────────────────────────
    let workingTree = null;   // live in-memory state (what the user edits)
    let savedTree   = null;   // snapshot of last save — used for Cancel
    let editMode    = false;
    let currentId   = null;
    let navHistory  = [];     // stack of node IDs visited
    let clickedOptions = [];  // option labels selected along the active path
    let faqArticles = [];
    let diagnosticSession = null;
    let createdTicket = null;
    
    // ── Helpers ──────────────────────────────────────────────────
    const uid   = ()  => crypto.randomUUID();
    const clone = (x) => JSON.parse(JSON.stringify(x));
    
    function esc(s) {
        return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;')
                               .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }
    function escA(s) {
        return String(s ?? '').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
    }
    
    function T()    { return workingTree; }
    function N(id)  { return T() ? T().nodes[id] : null; }
    function cur()  { return N(currentId); }
    
    // ── Toast ────────────────────────────────────────────────────
    function toast(msg) {
        const el = document.getElementById('dtToast');
        if (!el) {
            console.info('[DiagnosticTree]', msg);
            return;
        }
        el.textContent = msg;
        el.classList.add('show');
        clearTimeout(el._t);
        el._t = setTimeout(() => el.classList.remove('show'), 2400);
    }
    
    // ── CSRF ─────────────────────────────────────────────────────
    function csrfHeaders() {
        const name  = document.querySelector('meta[name="_csrf_header"]')?.content;
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        return (name && token) ? { [name]: token } : {};
    }
    
    // ── API ──────────────────────────────────────────────────────
    async function fetchTree() {
        try {
            const res = await fetch(`${API}/tree`);
            if (res.status === 204 || res.status === 404) return null;
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return await res.json();
        } catch (e) {
            console.error('[DiagnosticTree] fetchTree failed:', e);
            return null;
        }
    }

    async function fetchFaqArticles() {
        try {
            const res = await fetch(`${API}/knowledge?size=100&sort=title,asc`);
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const page = await res.json();
            return page.content ?? [];
        } catch (e) {
            console.error('[DiagnosticTree] fetchFaqArticles failed:', e);
            return [];
        }
    }

    async function startDiagnosticSession() {
        if (!IS_CUSTOMER) return null;
        try {
            const res = await fetch(`${API}/diagnostics/sessions`, {
                method: 'POST',
                headers: csrfHeaders()
            });
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return await res.json();
        } catch (e) {
            console.error('[DiagnosticTree] startDiagnosticSession failed:', e);
            toast('The guided session could not be started. You can still browse the tree.');
            return null;
        }
    }
    
    async function pushTree() {
        // Build the payload in the format the controller expects
        const payload = {
            rootId: T().rootId,
            nodes:  {}
        };
        Object.entries(T().nodes).forEach(([id, n]) => {
            payload.nodes[id] = {
                type:    n.type,
                text:    n.text,
                knowledgeArticleId: n.knowledgeArticleId ?? null,
                options: (n.options ?? []).map(o => ({
                    id:     o.id,
                    label:  o.label,
                    nextId: o.nextId ?? null
                }))
            };
        });
    
        const res = await fetch(`${API}/tree`, {
            method:  'PUT',
            headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
            body:    JSON.stringify(payload)
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        return await res.json();
    }
    
    // ── Default tree (used when API returns nothing) ──────────────
    function buildDefaultTree() {
        const rootId = uid();
        const nodes = {};
    
        const configuredCategories = typeof SERVER_CATEGORIES !== 'undefined'
                && Array.isArray(SERVER_CATEGORIES) && SERVER_CATEGORIES.length
            ? SERVER_CATEGORIES
            : DEFAULT_CATEGORIES;

        nodes[rootId] = {
            id: rootId,
            type: 'question',
            text: 'What is the category of your complaint?',
            isRoot: true,
            knowledgeArticleId: null,
            options: configuredCategories.map(category => ({
                id: uid(),
                label: typeof category === 'string' ? category : category.name,
                nextId: null
            }))
        };
    
        return {
            rootId,
            nodes
        };
    }
    
    // ── Navigation ───────────────────────────────────────────────
    async function go(nextId, optionId) {
        if (!nextId) { toast('This option is unlinked — connect it in Edit mode'); return; }

        const selectedOption = cur()?.options?.find(option =>
            String(option.id) === String(optionId));

        if (IS_CUSTOMER && diagnosticSession?.id) {
            try {
                const res = await fetch(`${API}/diagnostics/sessions/${diagnosticSession.id}/answers`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
                    body: JSON.stringify({ questionId: currentId, optionId })
                });
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                diagnosticSession = await res.json();
            } catch (e) {
                console.error('[DiagnosticTree] answer failed:', e);
                toast('Your answer could not be saved. Please try again.');
                return;
            }
        }
        navHistory.push(currentId);
        if (selectedOption?.label) clickedOptions.push(selectedOption.label);
        currentId = diagnosticSession?.currentQuestion?.id ?? nextId;
        render();
    }
    
    function back() {
        if (IS_CUSTOMER) {
            toast('Use Start over to begin a new guided session.');
            return;
        }
        if (!navHistory.length) return;
        currentId = navHistory.pop();
        clickedOptions.pop();
        render();
    }
    
    
    // ── Edit: question / resolution text ─────────────────────────
    function setNodeText(nodeId, val) {
        N(nodeId).text = val;
    }
    
    // ── Edit: option label ────────────────────────────────────────
    function setOptLabel(nodeId, optId, val) {
        const o = N(nodeId).options.find(o => o.id === optId);
        if (o) o.label = val;
    }

    function setKnowledgeArticle(nodeId, value) {
        const node = N(nodeId);
        if (node) node.knowledgeArticleId = value ? Number(value) : null;
    }
    
   
    
    // ── Edit: delete one option ───────────────────────────────────
    function deleteOption(nodeId, optId) {
        const n = N(nodeId);
        n.options = n.options.filter(o => o.id !== optId);
        render();
        toast('Option removed');
    }
    
    // ── Edit: add blank option ────────────────────────────────────
    function addOption(nodeId) {
        const n = N(nodeId);
        if ((n.options ?? []).length >= MAX_OPTS) {
            toast(`Maximum ${MAX_OPTS} options per question`);
            return;
        }
        if (!n.options) n.options = [];
        n.options.push({ id: uid(), label: 'New option', nextId: null });
        render();
    }
    
    // ── Edit: add question, link from option, navigate to it ──────
    function addQuestion(nodeId, optId) {
        const opt = N(nodeId).options.find(o => o.id === optId);
        if (!opt) return;
        if (opt.nextId) {
            toast('Already linked — set "leads to" to unlinked first');
            return;
        }
        const newId = uid();
        T().nodes[newId] = {
            id: newId, type: 'question', text: 'New question',
            isRoot: false, knowledgeArticleId: null, options: []
        };
        opt.nextId = newId;
        navHistory.push(currentId);
        clickedOptions.push(opt.label);
        currentId = newId;
        render();
        toast('Question added — edit the text above');
    }
    
    // ── Edit: add resolution, link from option, navigate to it ────
    function addResolution(nodeId, optId) {
        const opt = N(nodeId).options.find(o => o.id === optId);
        if (!opt) return;
        if (opt.nextId) {
            toast('Already linked — set "leads to" to unlinked first');
            return;
        }
        const newId = uid();
        T().nodes[newId] = {
            id: newId, type: 'resolution',
            text: 'Describe the resolution or suggested fix here.',
            isRoot: false, knowledgeArticleId: null, options: []
        };
        opt.nextId = newId;
        navHistory.push(currentId);
        clickedOptions.push(opt.label);
        currentId = newId;
        render();
        toast('Resolution added — edit the text above');
    }
    
    // ── Edit: cascade delete current node ────────────────────────
    function collectDescendants(startId) {
        const visited = new Set();
        const queue   = [startId];
        while (queue.length) {
            const id = queue.shift();
            if (visited.has(id)) continue;
            visited.add(id);
            const n = N(id);
            if (n?.options) n.options.forEach(o => { if (o.nextId) queue.push(o.nextId); });
        }
        return visited;
    }
    
    function deleteCurrentNode() {
        if (!T() || currentId === T().rootId) {
            toast("Can't delete the root node");
            return;
        }
        const toDelete = collectDescendants(currentId);
        const extra    = toDelete.size - 1;
        const msg = extra > 0
            ? `Delete this node and ${extra} connected node${extra > 1 ? 's' : ''} it leads to?`
            : 'Delete this node?';
        if (!confirm(msg)) return;
    
        // Unlink options in surviving nodes that point to deleted nodes
        Object.values(T().nodes).forEach(n => {
            if (toDelete.has(n.id)) return;
            (n.options ?? []).forEach(o => {
                if (o.nextId && toDelete.has(o.nextId)) o.nextId = null;
            });
        });
    
        toDelete.forEach(id => delete T().nodes[id]);
        toast(`Deleted ${toDelete.size} node${toDelete.size > 1 ? 's' : ''}`);
    
        if (navHistory.length) {
            currentId = navHistory.pop();
            clickedOptions.pop();
        } else {
            currentId  = T().rootId;
            navHistory = [];
            clickedOptions = [];
        }
        render();
    }
    
    // ── Save / Cancel ─────────────────────────────────────────────
    async function saveChanges() {
        const btn = document.querySelector('.dt-nav-btn--save');
        if (btn) { btn.disabled = true; btn.textContent = 'Saving…'; }
        try {
            const result  = await pushTree();
            workingTree   = result;               // replace with what server returned
            savedTree     = clone(result);
            currentId     = workingTree.rootId;
            navHistory    = [];
            clickedOptions = [];
            toast('Changes saved ✓');
        } catch (e) {
            toast('Save failed — see console');
            console.error('[DiagnosticTree] saveChanges:', e);
        } finally {
            render();
        }
    }
    
    function cancelChanges() {
        workingTree = clone(savedTree);
        currentId   = workingTree ? workingTree.rootId : null;
        navHistory  = [];
        clickedOptions = [];
        render();
        toast('Changes discarded');
    }

    async function restartCustomer() {
        diagnosticSession = await startDiagnosticSession();
        createdTicket = null;
        navHistory = [];
        clickedOptions = [];
        currentId = diagnosticSession?.currentQuestion?.id ?? T().rootId;
        render();
    }

    async function createTicket() {
        const node = cur();
        if (!node || createdTicket) return;
        const button = document.querySelector('.dt-ticket-btn');
        if (button) {
            button.disabled = true;
            button.textContent = 'Creating ticket…';
        }

        try {
            if (diagnosticSession?.status === 'SOLUTION_SUGGESTED') {
                const resolution = await fetch(
                    `${API}/diagnostics/sessions/${diagnosticSession.id}/resolution`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
                        body: JSON.stringify({ resolved: false })
                    });
                if (!resolution.ok) throw new Error(`Resolution HTTP ${resolution.status}`);
                diagnosticSession = await resolution.json();
            }

            const res = await fetch(`${API}/tickets`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', ...csrfHeaders() },
                body: JSON.stringify({
                    subject: `Support needed after diagnostic: ${node.text}`.slice(0, 200),
                    description: `The suggested diagnostic resolution did not resolve the issue.\n\n${node.text}`,
                    project: 'Diagnostic Support',
                    priority: 'MEDIUM',
                    diagnosticSessionId: diagnosticSession?.id ?? null,
                    suggestedArticleId: diagnosticSession?.id ? null : (node.knowledgeArticleId ?? null)
                })
            });
            if (!res.ok) throw new Error(`Ticket HTTP ${res.status}`);
            createdTicket = await res.json();
            toast(`Ticket ${createdTicket.referenceNumber} created`);
        } catch (e) {
            console.error('[DiagnosticTree] createTicket failed:', e);
            toast('The ticket could not be created. Please try again.');
        } finally {
            render();
        }
    }
    
    // ── Render ────────────────────────────────────────────────────
    function render() {
        renderTopBar();
        renderCard();
        renderNavBar();
    }
    
    function renderTopBar() {
        const parts = ['Start', ...clickedOptions];
    
        const bcHtml = parts.map((p, i) =>
            i < parts.length - 1
                ? `${esc(p)} <span class="dt-bc-sep">›</span> `
                : esc(p)
        ).join('');
    
        document.getElementById('dtTopBar').innerHTML = `
            <div class="dt-bc">${bcHtml}</div>
            <div class="dt-step">Step ${navHistory.length + 1}</div>`;
    }
    
    function renderCard() {
        const area = document.getElementById('dtCard');
        if (!T()) {
            area.innerHTML = `
                <div class="dt-empty">
                    <h3>No tree found</h3>
                    <p>Switch to Edit mode and save to create the initial tree.</p>
                </div>`;
            return;
        }
        const n = cur();
        if (!n) {
            area.innerHTML = `<div class="dt-empty"><h3>Node not found</h3></div>`;
            return;
        }
        n.type === 'resolution' ? renderResolution(area, n) : renderQuestion(area, n);
    }
    

    function renderResolution(area, n) {
        const isRoot = currentId === T().rootId;

        const bodyHtml = editMode
            ? `<textarea class="dt-res-textarea"
                         oninput="DT.setNodeText('${n.id}',this.value)">${esc(n.text)}</textarea>`
            : `<div class="dt-res-text">${esc(n.text)}</div>`;

        const relatedFaq = faqArticles.find(article =>
            String(article.id) === String(n.knowledgeArticleId));
        const faqOptions = [
            '<option value="">No related FAQ</option>',
            ...faqArticles.map(article => `<option value="${article.id}" ${
                String(article.id) === String(n.knowledgeArticleId) ? 'selected' : ''
            }>${esc(article.title)}</option>`)
        ].join('');
        const relatedFaqHtml = !editMode && relatedFaq ? `
            <div class="dt-faq-section">
                <strong>Related FAQ:</strong>
                <a href="${IS_CUSTOMER ? '/customer/faq' : '/admin/faq'}?article=${relatedFaq.id}">${esc(relatedFaq.title)}</a>
            </div>` : '';

        const ticketBtn = IS_CUSTOMER ? (createdTicket ? `
            <div class="dt-ticket-section">
                <div class="dt-ticket-copy">
                    <strong>Ticket ${esc(createdTicket.referenceNumber)} created</strong>
                    <span>Our support team has received your diagnostic history.</span>
                </div>
            </div>` : `
            <div class="dt-ticket-section">
                <div class="dt-ticket-content">
                    <div class="dt-ticket-icon">⚠️</div>
                    <div class="dt-ticket-copy">
                        <strong>Issue not resolved?</strong>
                        <span>If this solution didn't resolve your issue, our support team can help.</span>
                    </div>
                </div>

                <button class="dt-ticket-btn" onclick="DT.createTicket()">
                    Raise a Ticket
                </button>
            </div>
        `) : '';

        const editBar = editMode && !isRoot ? `
            <div class="dt-edit-bar">
                <button class="dt-bar-btn dt-bar-btn--red"
                        onclick="DT.deleteCurrentNode()">🗑 Delete resolution</button>
            </div>` : '';
    
        area.innerHTML = `
                <div class="dt-res-card">
                <div class="dt-res-lbl">✓ Resolution found</div>
                ${bodyHtml}
                ${relatedFaqHtml}

                ${editMode ? `
                    <div class="dt-faq-section">
                        <label class="dt-faq-label">Related FAQ</label>

                        <select class="dt-faq-select"
                                onchange="DT.setKnowledgeArticle('${n.id}', this.value)">
                            ${faqOptions}
                        </select>
                    </div>
                ` : ''}

                ${editBar}
            </div>
            ${ticketBtn}`;
    }
    
    function renderQuestion(area, n) {
        const isRoot   = currentId === T().rootId;
        const qNum     = navHistory.length + 1;
        const opts     = n.options ?? [];
        const atMax    = opts.length >= MAX_OPTS;
    
        const optsHtml = opts.map((o, i) => {
            const ltr     = LETTERS[i] ?? '?';
            const linked  = !!o.nextId;
    
            if (editMode) {
                const disabledAttr = linked
                    ? 'disabled title="Already linked"'
                    : '';
            
                return `
                <div class="dt-opt dt-opt--edit">
                    <div class="dt-opt-top">
                        <span class="dt-letter">${ltr}</span>
            
                        <input
                            class="dt-opt-input"
                            type="text"
                            value="${escA(o.label)}"
                            oninput="DT.setOptLabel('${n.id}','${o.id}',this.value)"
                            placeholder="Option text…">
            
                        <button
                            class="dt-del"
                            onclick="DT.deleteOption('${n.id}','${o.id}')"
                            aria-label="Delete option">
                            ✕
                        </button>
                    </div>
            
                    <div class="dt-next-row">
            
                        <button
                            class="dt-mini dt-mini--q"
                            ${disabledAttr}
                            onclick="DT.addQuestion('${n.id}','${o.id}')">
                            + New Question
                        </button>
            
                        <button
                            class="dt-mini dt-mini--r"
                            ${disabledAttr}
                            onclick="DT.addResolution('${n.id}','${o.id}')">
                            + Resolution
                        </button>
            
                    </div>
                </div>`;
            }
    
            // Preview mode
            return `
                <div class="dt-opt"
                     role="button" tabindex="0"
                     onclick="DT.go('${o.nextId ?? ''}','${o.id}')"
                     onkeydown="if(event.key==='Enter'||event.key===' ')DT.go('${o.nextId ?? ''}','${o.id}')">
                    <span class="dt-letter">${ltr}</span>
                    <span class="dt-opt-label">${esc(o.label)}</span>
                    ${o.nextId
                        ? `<span class="dt-arrow">›</span>`
                        : `<span class="dt-unlinked">unlinked</span>`}
                </div>`;
        }).join('');
    
        const countHtml = editMode ? `
            <div class="dt-opt-count ${atMax ? 'at-max' : ''}">
                ${opts.length} / ${MAX_OPTS} options${atMax ? ' — maximum reached' : ''}
            </div>` : '';
    
        const addOptHtml = editMode ? `
            <button class="dt-add-opt" ${atMax ? 'disabled' : ''}
                    onclick="DT.addOption('${n.id}')">＋ Add option</button>` : '';
    
        const editBarHtml = editMode ? `
            <div class="dt-edit-bar">
                ${!isRoot
                    ? `<button class="dt-bar-btn dt-bar-btn--red"
                               onclick="DT.deleteCurrentNode()">
                               🗑️ Delete Question + Everything After
                       </button>`
                    : ''}
            </div>` : '';
    
        area.innerHTML = `
            <div class="dt-q-card">
                <div class="dt-q-num">Question ${qNum}</div>
                ${editMode
                    ? `<textarea class="dt-q-textarea"
                                 oninput="DT.setNodeText('${n.id}',this.value)">${esc(n.text)}</textarea>`
                    : `<div class="dt-q-text">${esc(n.text)}</div>`}
                <div class="dt-opts">
                    ${optsHtml}
                    ${addOptHtml}
                </div>
                ${countHtml}
                ${editBarHtml}
            </div>`;
    }
    
    function renderNavBar() {
        document.getElementById('dtNavBar').innerHTML = `
            <button class="dt-nav-btn"
                    onclick="${IS_CUSTOMER ? 'DT.restartCustomer()' : 'DT.back()'}"
                    ${!IS_CUSTOMER && !navHistory.length ? 'disabled' : ''}>
                    ${IS_CUSTOMER ? '↻ Start over' : '← Back'}</button>
            ${editMode ? `
            <button class="dt-nav-btn dt-nav-btn--cancel"
                    onclick="DT.cancelChanges()">Cancel</button>
            <button class="dt-nav-btn dt-nav-btn--save"
                    onclick="DT.saveChanges()">Save changes</button>
            ` : ''}`;
    }
  
    // ── Edit toggle ───────────────────────────────────────────────
    const editBtn = document.getElementById('dtEditBtn');

        if (editBtn) {
            editBtn.addEventListener('click', () => {
                editMode = !editMode;

                editBtn.textContent = editMode
                    ? '✓ Editing'
                    : '✎ Edit tree';

                editBtn.classList.toggle('on', editMode);

                render();
            });
        }
    
    // ── Boot ──────────────────────────────────────────────────────
    (async function init() {
        const [remote, articles] = await Promise.all([fetchTree(), fetchFaqArticles()]);
        faqArticles = articles;
        if (remote && remote.rootId) {
            workingTree = remote;
            savedTree   = clone(remote);
        } else {
            workingTree = buildDefaultTree();
            savedTree   = clone(workingTree);
        }
        diagnosticSession = await startDiagnosticSession();
        currentId = diagnosticSession?.currentQuestion?.id ?? workingTree.rootId;
        render();
    })();
    
    // ── Public API (used by inline onclick handlers) ──────────────
    window.DT = {
        go, back,
        setNodeText, setOptLabel, setKnowledgeArticle,
        addOption, deleteOption,
        addQuestion, addResolution,
        deleteCurrentNode,
        saveChanges, cancelChanges, restartCustomer, createTicket,
        toast
    };
    
    })();