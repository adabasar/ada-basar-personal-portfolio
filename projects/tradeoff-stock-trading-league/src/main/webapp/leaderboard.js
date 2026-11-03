let currentUserId = null;
let sessionUsername = null;

/* DOM Elements */
const tabGlobal = document.getElementById("tab-global");
const tabLeague = document.getElementById("tab-league");
const leaderboardRows = document.getElementById("leaderboardRows");
const updatedAgoEl = document.getElementById("updatedAgo");
const userDetails = document.getElementById("userDetails");

const POLL_INTERVAL_MS = 15000;
const PAGE_SIZE = 100;

let lastFetchTime = null;
let pollTimer = null;
let currentType = "global";
let latestData = [];

/* ---------------------------------------------------------
   INIT — Called after header loads
--------------------------------------------------------- */
async function initLeaderboardPage() {
    try {
        // 1. Check session
        const sessionResp = await fetch("user-auth?action=check-session");
        const sessionData = await sessionResp.json();

        if (!sessionData.loggedIn) {
            window.location.href = "login.html";
            return;
        }

        currentUserId = sessionData.userID;
        sessionUsername = sessionData.username;

        // 2. Fetch and render leaderboard
        startPolling();

    } catch (err) {
        console.error("Error initializing leaderboard page:", err);
    }
}

/* ---------------------------------------------------------
   LEADERBOARD FETCH
--------------------------------------------------------- */
async function fetchGlobalLeaderboard() {
    try {
        const resp = await fetch(`leaderboard?page=1&pageSize=${PAGE_SIZE}`, { 
            cache: "no-store" 
        });
        
        if (!resp.ok) {
            throw new Error("Fetch failed: " + resp.status);
        }
        
        const data = await resp.json();
        
        if (!data.success) {
            console.error("Leaderboard error:", data.message);
            return [];
        }
        
        return data.rankings || [];
        
    } catch (e) {
        console.error("Error fetching global leaderboard:", e);
        return [];
    }
}

async function fetchLeagueLeaderboard() {
    try {
        // First get user's league
        const leagueResp = await fetch(`league?action=user-league&userId=${currentUserId}`, {
            cache: "no-store"
        });
        
        const leagueData = await leagueResp.json();
        
        if (!leagueData.success || !leagueData.league) {
            console.log("User not in a league");
            return [];
        }
        
        const leagueId = leagueData.league.leagueId;
        
        // Get league rankings
        const rankingsResp = await fetch(`league?action=rankings&leagueId=${leagueId}`, {
            cache: "no-store"
        });
        
        const rankingsData = await rankingsResp.json();
        
        if (!rankingsData.success) {
            console.error("Rankings error:", rankingsData.message);
            return [];
        }
        
        // Transform league data to match global format
        const rankings = rankingsData.rankings || [];
        return rankings.map((member, index) => ({
            rank: index + 1,
            userId: member.userId,
            username: member.username,
            displayName: member.displayName,
            portfolioValue: member.portfolioValue,
            profitLoss: member.profitLoss,
            weeklyProfitLoss: member.weeklyProfitLoss,
            percentChange: ((member.profitLoss / 100000) * 100) // Calculate percentage
        }));
        
    } catch (e) {
        console.error("Error fetching league leaderboard:", e);
        return [];
    }
}

async function fetchLeaderboard() {
    if (currentType === "global") {
        return await fetchGlobalLeaderboard();
    } else {
        return await fetchLeagueLeaderboard();
    }
}

/* ---------------------------------------------------------
   RENDER HELPERS
--------------------------------------------------------- */
function formatCurrency(n) {
    if (typeof n !== "number") n = Number(n) || 0;
    return "$" + n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatSignedPercent(n) {
    if (typeof n !== "number") n = Number(n) || 0;
    return (n >= 0 ? "+" : "") + n.toFixed(2) + "%";
}

function escapeHtml(str) {
    if (!str && str !== 0) return "";
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function buildRow(item) {
    const tr = document.createElement("tr");
    const displayName = item.displayName || item.username || "Unknown";
    const portfolioValue = item.portfolioValue || 0;
    const percentChange = item.percentChange || 0;
    
    tr.dataset.username = item.username || "";
    tr.dataset.pctChange = percentChange;
    
    tr.innerHTML = `
        <td class="col-rank">${escapeHtml(item.rank)}</td>
        <td class="col-user clickable">${escapeHtml(displayName)}</td>
        <td class="col-equity">${formatCurrency(portfolioValue)}</td>
        <td class="col-pct ${percentChange > 0 ? 'positive' : (percentChange < 0 ? 'negative' : 'neutral')}">
            ${formatSignedPercent(percentChange)}
        </td>
    `;
    
    tr.querySelector(".col-user").addEventListener("click", () => loadUserDetails(item.username));
    
    return tr;
}

function renderTable(data) {
    leaderboardRows.innerHTML = "";
    
    if (data.length === 0) {
        leaderboardRows.innerHTML = `
            <tr><td colspan="4" style="text-align:center;color:#9ca3af;padding:20px;">
                ${currentType === "league" ? "You are not in a league" : "No data available"}
            </td></tr>
        `;
        return;
    }
    
    data.forEach(item => {
        const row = buildRow(item);
        leaderboardRows.appendChild(row);
    });
}

function renderUserPlaceholder() {
    userDetails.dataset.currentUsername = "";
    userDetails.innerHTML = `<div class="user-empty"><p>No user selected</p></div>`;
}

function renderUserDetails(profile) {
    const username = profile.username || "Unknown";
    const displayName = profile.displayName || username;
    const pic = profile.profilePicUrl || "default_profile.jpg";
    const isVisible = Boolean(profile.profileVisibility);
    const capital = typeof profile.availableCapital !== "undefined" ? Number(profile.availableCapital) : null;

    userDetails.dataset.currentUsername = username;

    let capitalHtml = isVisible && capital !== null
        ? `<div class="user-capital"><div class="label">Available capital</div><div class="value">${formatCurrency(capital)}</div></div>`
        : `<div class="user-capital"><div class="label">Available capital</div><div class="value muted">Hidden</div></div>`;

    userDetails.innerHTML = `
        <div class="user-top">
            <img class="user-avatar" src="${escapeHtml(pic)}" alt="${escapeHtml(displayName)}'s profile picture" />
            <div class="user-meta">
                <div class="user-name">${escapeHtml(displayName)}</div>
            </div>
        </div>
        ${capitalHtml}
    `;
}

/* ---------------------------------------------------------
   USER DETAILS FETCH
--------------------------------------------------------- */
async function loadUserDetails(username, force = false) {
    if (!username) return;
    if (!force && userDetails.dataset.currentUsername === username) return;

    try {
        // Get user ID from leaderboard data
        const userItem = latestData.find(x => x.username === username);
        if (!userItem) {
            renderUserPlaceholder();
            return;
        }
        
        const userId = userItem.userId;
        
        // Fetch full profile
        const res = await fetch(`profile?userId=${encodeURIComponent(userId)}`);
        const data = await res.json();

        if (data.success && data.profile) {
            renderUserDetails(data.profile);
        } else {
            // Fallback to leaderboard data
            renderUserDetails({
                username: userItem.username,
                displayName: userItem.displayName,
                profilePicUrl: "default_profile.jpg",
                profileVisibility: false,
                availableCapital: userItem.portfolioValue
            });
        }
    } catch (err) {
        console.error("Error fetching user details:", err);
        renderUserPlaceholder();
    }
}

/* ---------------------------------------------------------
   TAB & POLLING
--------------------------------------------------------- */
function setActiveTab(type) {
    currentType = type;
    if (type === "global") {
        tabGlobal.classList.add("active");
        tabGlobal.setAttribute("aria-selected", "true");
        tabLeague.classList.remove("active");
        tabLeague.setAttribute("aria-selected", "false");
    } else {
        tabLeague.classList.add("active");
        tabLeague.setAttribute("aria-selected", "true");
        tabGlobal.classList.remove("active");
        tabGlobal.setAttribute("aria-selected", "false");
    }
    fetchAndRender();
}

async function fetchAndRender() {
    const data = await fetchLeaderboard();
    latestData = data;
    renderTable(data);
    lastFetchTime = new Date();
    updateUpdatedAgo();

    if (data.length > 0) {
        const topUser = data[0].username;
        loadUserDetails(topUser, true);
    } else {
        renderUserPlaceholder();
    }
}

function updateUpdatedAgo() {
    if (!lastFetchTime) {
        updatedAgoEl.textContent = "never";
        return;
    }
    const diffS = Math.floor((new Date() - lastFetchTime) / 1000);
    let text = "";
    if (diffS < 5) text = "just now";
    else if (diffS < 60) text = diffS + "s ago";
    else if (diffS < 3600) text = Math.floor(diffS / 60) + "m ago";
    else text = Math.floor(diffS / 3600) + "h ago";

    updatedAgoEl.textContent = text;

    const next = diffS < 60 ? 5000 : diffS < 3600 ? 60000 : 1800000;
    setTimeout(updateUpdatedAgo, next);
}

function startPolling() {
    if (pollTimer) clearInterval(pollTimer);
    fetchAndRender();
    pollTimer = setInterval(fetchAndRender, POLL_INTERVAL_MS);
}

/* ---------------------------------------------------------
   EVENT LISTENERS
--------------------------------------------------------- */
tabGlobal.addEventListener("click", () => setActiveTab("global"));
tabLeague.addEventListener("click", () => setActiveTab("league"));

/* ---------------------------------------------------------
   LOAD HEADER & INIT
--------------------------------------------------------- */
function loadContent(placeholderId, filePath) {
    return fetch(filePath)
        .then(r => r.text())
        .then(html => {
            document.getElementById(placeholderId).innerHTML = html;
        });
}

loadContent("header-placeholder", "header.html")
    .then(() => {
        if (typeof initHeader === "function") initHeader();
        initLeaderboardPage();
    });

window.addEventListener("pageshow", (e) => {
    if (e.persisted) initLeaderboardPage();
});