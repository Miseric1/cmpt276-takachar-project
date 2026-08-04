document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('spoc-customer-form');
    const formMessage = document.getElementById('spoc-form-message');
    const createButton = document.getElementById('spoc-create-button');
    const customerCount = document.getElementById('spoc-customer-count');
    const listCount = document.getElementById('spoc-list-count');
    const listError = document.getElementById('spoc-list-error');
    const emptyState = document.getElementById('spoc-customer-empty');
    const tableWrapper = document.getElementById('spoc-customer-table-wrapper');
    const customerList = document.getElementById('spoc-customer-list');

    const showFormMessage = (message, error) => {
        formMessage.textContent = message;
        formMessage.className = error
            ? 'spoc-form-message spoc-form-message--error'
            : 'spoc-form-message spoc-form-message--success';
        formMessage.hidden = false;
    };

    const renderCustomers = (customers) => {
        customerList.replaceChildren();

        customers.forEach(customer => {
            const row = document.createElement('tr');

            const emailCell = document.createElement('td');
            const email = document.createElement('strong');
            email.textContent = customer.email;
            emailCell.appendChild(email);

            const roleCell = document.createElement('td');
            roleCell.textContent = customer.role;

            const accessCell = document.createElement('td');
            const access = document.createElement('span');
            access.className = 'admin-status-badge admin-status-other';
            access.textContent = 'Active';
            accessCell.appendChild(access);

            row.append(emailCell, roleCell, accessCell);
            customerList.appendChild(row);
        });

        const count = customers.length;
        customerCount.textContent = String(count);
        listCount.textContent = `${count} ${count === 1 ? 'account' : 'accounts'}`;
        emptyState.hidden = count !== 0;
        tableWrapper.hidden = count === 0;
        listError.hidden = true;
    };

    const loadCustomers = async () => {
        try {
            const response = await fetch('/api/admin/customers', { credentials: 'same-origin' });
            if (!response.ok) throw new Error('Could not load accounts');
            renderCustomers(await response.json());
        } catch (error) {
            customerCount.textContent = '—';
            listCount.textContent = 'Unavailable';
            listError.hidden = false;
            emptyState.hidden = true;
            tableWrapper.hidden = true;
        }
    };

    form.addEventListener('submit', async event => {
        event.preventDefault();
        formMessage.hidden = true;
        createButton.disabled = true;
        createButton.textContent = 'Creating account…';

        const payload = {
            email: form.elements.email.value.trim(),
            password: form.elements.password.value,
            role: form.elements.role.value
        };

        try {
            const response = await fetch('/api/admin/customers', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const body = await response.json().catch(() => ({}));
                throw new Error(body.message || 'The account could not be created.');
            }

            const customer = await response.json();
            form.reset();
            const roleLabel = customer.role.toLowerCase();
            showFormMessage(`${roleLabel.charAt(0).toUpperCase() + roleLabel.slice(1)} access created for ${customer.email}.`, false);
            await loadCustomers();
        } catch (error) {
            showFormMessage(error.message, true);
        } finally {
            createButton.disabled = false;
            createButton.textContent = 'Create account';
        }
    });

    loadCustomers();
});
