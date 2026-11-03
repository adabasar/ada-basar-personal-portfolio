const loginTab = document.getElementById('loginTab');
const registerTab = document.getElementById('registerTab');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const errorMessage = document.getElementById('errorMessage');
const successMessage = document.getElementById('successMessage');

const loginFormElement = document.getElementById('loginFormElement');
const loginUsername = document.getElementById('loginUsername');
const loginPassword = document.getElementById('loginPassword');
const loginBtn = document.getElementById('loginBtn');

const registerFormElement = document.getElementById('registerFormElement');
const registerUsername = document.getElementById('registerUsername');
const registerDisplayName = document.getElementById('registerDisplayName');
const registerPassword = document.getElementById('registerPassword');
const registerConfirmPassword = document.getElementById('registerConfirmPassword');
const registerBtn = document.getElementById('registerBtn');

loginTab.addEventListener('click', () => {
    loginTab.classList.add('active');
    registerTab.classList.remove('active');
    loginForm.classList.add('active');
    registerForm.classList.remove('active');
    clearMessages();
});

registerTab.addEventListener('click', () => {
    registerTab.classList.add('active');
    loginTab.classList.remove('active');
    registerForm.classList.add('active');
    loginForm.classList.remove('active');
    clearMessages();
});

loginFormElement.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearMessages();

    const username = loginUsername.value.trim();
    const password = loginPassword.value.trim();

    if (!username || !password) {
        showError('Please enter both username and password');
        return;
    }

    setLoading(loginBtn, true);

    try {
        const response = await fetch('/user-auth', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'login',
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (data.success) {
            showSuccess('Login successful! Redirecting...');
            
        } else {
            showError(data.message || 'Login failed. Please check your credentials.');
        }
    } catch (error) {
        console.error('Login error:', error);
        showError('An error occurred. Please try again.');
    } finally {
        setLoading(loginBtn, false);
    }
});

registerFormElement.addEventListener('submit', async (e) => {
    e.preventDefault();
    clearMessages();

    const username = registerUsername.value.trim();
    const displayName = registerDisplayName.value.trim();
    const password = registerPassword.value.trim();
    const confirmPassword = registerConfirmPassword.value.trim();

    if (!username || !displayName || !password || !confirmPassword) {
        showError('Please fill in all required fields');
        return;
    }

    if (username.length < 3) {
        showError('Username must be at least 3 characters');
        return;
    }

    if (password.length < 6) {
        showError('Password must be at least 6 characters');
        return;
    }

    if (password !== confirmPassword) {
        showError('Passwords do not match');
        return;
    }

    setLoading(registerBtn, true);

    try {
        const response = await fetch('/user-auth', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                action: 'register',
                username: username,
                displayName: displayName,
                password: password
            })
        });

        const data = await response.json();

        if (data.success) showSuccess('Account created successfully!');
            
        else showError(data.message || 'Registration failed. Please try again.');
        
    } catch (error) {
        console.error('Registration error:', error);
        showError('An error occurred. Please try again.');
    } finally {
        setLoading(registerBtn, false);
    }
});

function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
    successMessage.style.display = 'none';
}

function showSuccess(message) {
    successMessage.textContent = message;
    successMessage.style.display = 'block';
    errorMessage.style.display = 'none';
}

function clearMessages() {
    errorMessage.style.display = 'none';
    successMessage.style.display = 'none';
}

function setLoading(button, isLoading) {
    if (isLoading) {
        button.disabled = true;
        const originalText = button.textContent;
        button.dataset.originalText = originalText;
        button.textContent = originalText;  
    } else {
        button.disabled = false;
        button.textContent = button.dataset.originalText || button.textContent;
    }
}

async function checkSession() {
    try {
        const response = await fetch('/user-auth?action=check-session');
        const data = await response.json();

        if (data.loggedIn) window.location.href = 'home.html';
        
    } catch (error) {
        console.error('Session check error:', error);
    }
}

checkSession();