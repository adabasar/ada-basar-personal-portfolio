function initProfileDropdown() {
    const profileMenu = document.getElementById("profileMenu");
    const profileDropdown = document.getElementById("profileDropdown");

    if (!profileMenu || !profileDropdown) return;

    profileMenu.addEventListener("click", (e) => {
        e.stopPropagation();
        const isVisible = profileDropdown.style.display === "block";
        profileDropdown.style.display = isVisible ? "none" : "block";

        const notifDropdown = document.getElementById("notifDropdown");
        if (notifDropdown) notifDropdown.style.display = "none";
    });

    document.addEventListener("click", (event) => {
        if (!profileMenu.contains(event.target)) {
            profileDropdown.style.display = "none";
        }
    });
}

function initNotifications() {
    const notifMenu = document.getElementById("notifMenu");
    const notifButton = document.getElementById("notifButton");
    const notifDropdown = document.getElementById("notifDropdown");
    const notifList = document.getElementById("notifList");
    const notifBadge = document.getElementById("notifBadge");

    if (!notifButton || !notifDropdown || !notifList || !notifBadge) return;

    let notifications = Array.isArray(window.notifications) ? window.notifications : [];

    function renderNotifications() {
        notifList.innerHTML = "";

        if (!notifications || notifications.length === 0) {
            notifList.innerHTML = `<li class="notif-empty">No notifications</li>`;
            notifBadge.style.display = "none";
            return;
        }

        const unread = notifications.filter(n => !n.read).length;
        if (unread > 0) {
            notifBadge.style.display = "flex";
            notifBadge.textContent = unread > 9 ? "9+" : unread;
        } else {
            notifBadge.style.display = "none";
        }

        notifications.forEach(n => {
            const li = document.createElement("li");
            li.classList.add("notif-item");
            if (n.read) li.classList.add("notif-read");

            li.innerHTML = `
                <div class="notif-text">${n.text}</div>
                <div class="notif-time">${n.time || ""}</div>
            `;

            li.addEventListener("click", () => {
                n.read = true;
                renderNotifications();
                if (n.link) {
                    window.location.href = n.link;
                }
            });

            notifList.appendChild(li);
        });
    }

    window.updateNotifications = function(newNotifications) {
        notifications = Array.isArray(newNotifications) ? newNotifications : [];
        renderNotifications();
    };

    renderNotifications();

    notifButton.addEventListener("click", (e) => {
        e.stopPropagation();
        const visible = notifDropdown.style.display === "block";
        notifDropdown.style.display = visible ? "none" : "block";

        const profileDropdown = document.getElementById("profileDropdown");
        if (profileDropdown) profileDropdown.style.display = "none";
    });

    document.addEventListener("click", (e) => {
        if (!notifMenu.contains(e.target)) {
            notifDropdown.style.display = "none";
        }
    });
}

function controlHomeButton() {
    const homeLink = document.querySelector(".nav-home");
    if (!homeLink) return;

    const path = window.location.pathname.toLowerCase();
    const isHomePage =
        path.endsWith("home") ||
        path.endsWith("/home") ||
        path.endsWith("/home/") ||
        path.endsWith("/home.html");

    if (isHomePage) {
        homeLink.style.display = "none";
    }
}

function initHeader() {
    initProfileDropdown();
    initNotifications();
    controlHomeButton();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHeader);
} else {
    initHeader();
}
