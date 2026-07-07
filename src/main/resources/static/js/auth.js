document.addEventListener('DOMContentLoaded', function () {

    // password visibility toggle (works for any of the password fields on the page)
    document.querySelectorAll('.toggle-password').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const input = document.getElementById(btn.dataset.target);
            if (!input) return;

            const isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';

            btn.querySelector('.icon-eye').style.display = isHidden ? 'none' : 'block';
            btn.querySelector('.icon-eye-off').style.display = isHidden ? 'block' : 'none';
            btn.setAttribute('aria-label', isHidden ? 'Hide password' : 'Show password');
        });
    });

    // register page: confirm password match check
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', function (e) {
            const password = document.getElementById('password').value;
            const confirm = document.getElementById('confirmPassword').value;
            const matchError = document.getElementById('matchError');

            if (password !== confirm) {
                e.preventDefault();
                matchError.style.display = 'inline';
            } else {
                matchError.style.display = 'none';
            }
        });
    }
});