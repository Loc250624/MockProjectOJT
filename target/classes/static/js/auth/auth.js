document.addEventListener("DOMContentLoaded", function() {
    
    // Login Form Handler
    const loginForm = document.getElementById("loginForm");
    if (loginForm) {
        loginForm.addEventListener("submit", async function(e) {
            e.preventDefault();
            const submitBtn = this.querySelector('button[type="submit"]');
            submitBtn.classList.add('btn-loading');
            
            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;
            
            try {
                const response = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, password })
                });
                
                const data = await response.json();
                
                if (response.ok) {
                    const role = data.data.role;
                    if (role === 'ADMIN') window.location.href = '/admin/dashboard';
                    else if (role === 'TEACHER') window.location.href = '/teacher/dashboard';
                    else window.location.href = '/student/dashboard';
                } else {
                    alert(data.message || "Login failed");
                }
            } catch (error) {
                console.error("Error logging in:", error);
                alert("A system error occurred.");
            } finally {
                submitBtn.classList.remove('btn-loading');
            }
        });
    }

    // Register Form Handler
    const registerForm = document.getElementById("registerForm");
    if (registerForm) {
        registerForm.addEventListener("submit", async function(e) {
            e.preventDefault();
            const submitBtn = this.querySelector('button[type="submit"]');
            submitBtn.classList.add('btn-loading');
            
            const fullName = document.getElementById("fullName").value;
            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;
            const confirmPassword = document.getElementById("confirmPassword").value;
            
            if (password !== confirmPassword) {
                alert("Passwords do not match");
                submitBtn.classList.remove('btn-loading');
                return;
            }
            
            try {
                const response = await fetch("/api/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ fullName, email, password, confirmPassword })
                });
                
                const data = await response.json();
                
                if (response.ok) {
                    window.location.href = '/student/dashboard';
                } else {
                    alert(data.message || "Registration failed");
                }
            } catch (error) {
                console.error("Error registering:", error);
                alert("A system error occurred.");
            } finally {
                submitBtn.classList.remove('btn-loading');
            }
        });
    }

    // Logout Handler
    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", async function(e) {
            e.preventDefault();
            try {
                await fetch("/api/auth/logout", { method: "POST" });
                window.location.href = "/auth/login";
            } catch (error) {
                console.error("Error logging out:", error);
                window.location.href = "/auth/login";
            }
        });
    }
});
