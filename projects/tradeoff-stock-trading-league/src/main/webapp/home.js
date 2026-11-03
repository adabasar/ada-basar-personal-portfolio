// home.js

// 1. Load header.html into #header-placeholder and init header logic
function loadHeader() {
    const placeholder = document.getElementById("header-placeholder");
    if (!placeholder) return;

    fetch("header.html")
        .then(r => r.text())
        .then(html => {
            placeholder.innerHTML = html;
            if (typeof initHeader === "function") {
                initHeader();
            }
        });
}

async function getMarketHighlights(range) {
  const r = range || "1D";

  const series = {
    "1D": {
      labels: ["9:30", "11:00", "12:30", "2:00", "3:30", "Close"],
      values: [100, 100.6, 100.2, 101.1, 100.9, 101.4],
      topMovers: [
        { symbol: "AAPL", changePct: 1.35 },
        { symbol: "NVDA", changePct: 2.45 },
        { symbol: "TSLA", changePct: -0.85 }
      ]
    },
    "1W": {
      labels: ["Mon", "Tue", "Wed", "Thu", "Fri"],
      values: [100, 99.2, 100.1, 101.0, 101.7],
      topMovers: [
        { symbol: "META", changePct: 3.10 },
        { symbol: "MSFT", changePct: 1.20 },
        { symbol: "AMZN", changePct: -0.40 }
      ]
    },
    "1M": {
      labels: ["W1", "W2", "W3", "W4"],
      values: [100, 102.2, 101.4, 104.0],
      topMovers: [
        { symbol: "GOOGL", changePct: 4.15 },
        { symbol: "NFLX", changePct: 2.80 },
        { symbol: "JPM", changePct: -1.10 }
      ]
    }
  };

  return series[r] || series["1D"];
}


function initLeagueActions() {
    const body = document.body;
    const alreadyInSomeLeague = body.dataset.alreadyInSomeLeague === "true";

    const createBtn = document.getElementById("createLeagueBtn");
    const deleteBtn = document.getElementById("deleteLeagueBtn");
    const joinBtn = document.getElementById("joinLeagueBtn");
    const leaveBtn = document.getElementById("leaveLeagueBtn");
    const requestPrivateBtn = document.getElementById("requestPrivateLeagueBtn");
    const errorBox = document.getElementById("leagueErrorBox");

    function showError(msg) {
        if (!errorBox) return;
        errorBox.textContent = msg;
        errorBox.style.display = "block";
    }

    function clearError() {
        if (!errorBox) return;
        errorBox.textContent = "";
        errorBox.style.display = "none";
    }

    function guardSingleLeague(action) {
        clearError();
        if (alreadyInSomeLeague && action !== "leave" && action !== "delete") {
            showError("You can only be in one league at a time. Leave your current league before creating or joining another.");
            return false;
        }
        return true;
    }

    if (createBtn) {
        createBtn.addEventListener("click", () => {
            if (!guardSingleLeague("create")) return;
            // Redirect to your backend create page / endpoint
            window.location.href = "/league/create";
        });
    }

    if (joinBtn) {
        joinBtn.addEventListener("click", () => {
            if (!guardSingleLeague("join")) return;
            window.location.href = "/league/browse?type=public";
        });
    }

    if (requestPrivateBtn) {
        requestPrivateBtn.addEventListener("click", () => {
            if (!guardSingleLeague("request-private")) return;
            window.location.href = "/league/browse?type=private";
        });
    }

    if (leaveBtn) {
        leaveBtn.addEventListener("click", () => {
            clearError();
            if (confirm("Leave your current league?")) {
                window.location.href = "/league/leave";
            }
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener("click", () => {
            clearError();
            if (confirm("Delete your league for all members? This cannot be undone.")) {
                window.location.href = "/league/delete";
            }
        });
    }
}

function initHomeNavButtons() {
    const container = document.querySelector(".home-nav-buttons");
    if (!container) return;

    container.addEventListener("click", (e) => {
        const btn = e.target.closest("button[data-link]");
        if (!btn) return;
        const link = btn.dataset.link;
        if (link) window.location.href = link;
    });
}

function initHomeNotifications() {
    const isAdmin = document.body.dataset.isAdmin === "true";

    const baseNotifications = [
        {
            text: "Market update: NASDAQ up 1.2% today.",
            time: "5 min ago",
            read: false
        },
        {
            text: "Your rank in Global leaderboard moved to #12.",
            time: "30 min ago",
            read: false,
            link: "leaderboard.html"
        },
        {
            text: "Friend request from user 'StockWizard'.",
            time: "1 hr ago",
            read: true
        }
    ];

    if (isAdmin) {
        baseNotifications.unshift({
            text: "League request: user 'AlphaTrader' wants to join 'Tech Titans'.",
            time: "2 min ago",
            read: false,
            link: "league.html"
        });
    }

    window.notifications = baseNotifications;
    if (typeof updateNotifications === "function") {
        updateNotifications(baseNotifications);
    }
}

let marketChart;

function initMarketChart() {
    const ctx = document.getElementById("marketChart");
    const loadingEl = document.getElementById("marketChartLoading");
    const errorEl = document.getElementById("marketChartError");
    const topMoversList = document.getElementById("topMoversList");
    const rangeButtons = document.querySelectorAll("#marketRangeGroup .btn-pill");

    if (!ctx || !loadingEl || !errorEl || !topMoversList) return;

    function setLoading(isLoading) {
        loadingEl.style.display = isLoading ? "flex" : "none";
        errorEl.style.display = "none";
    }

    function setError(msg) {
        loadingEl.style.display = "none";
        errorEl.style.display = "flex";
        errorEl.textContent = msg || "Could not load market data.";
    }

    function renderTopMovers(movers) {
        topMoversList.innerHTML = "";
        if (!movers || movers.length === 0) {
            topMoversList.textContent = "No movers data available.";
            return;
        }
        movers.forEach(m => {
            const row = document.createElement("div");
            row.className = "top-movers-row";
            row.innerHTML = `
                <span class="top-mover-name">${m.symbol}</span>
                <span class="top-mover-change ${m.changePct >= 0 ? "positive" : "negative"}">
                    ${m.changePct >= 0 ? "+" : ""}${m.changePct.toFixed(2)}%
                </span>
            `;
            topMoversList.appendChild(row);
        });
    }

    function renderChart(labels, values) {
        if (marketChart) {
            marketChart.destroy();
        }
        marketChart = new Chart(ctx.getContext("2d"), {
            type: "line",
            data: {
                labels,
                datasets: [
                    {
                        label: "Index",
                        data: values,
                        fill: false,
                        tension: 0.25
                    }
                ]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { maxTicksLimit: 6 } }
                }
            }
        });
    }

    async function loadMarketData(range) {
        setLoading(true);
        try {
            const payload = await getMarketHighlights(range);

            renderChart(payload.labels, payload.values);
            renderTopMovers(payload.topMovers);
            setLoading(false);
        } catch (err) {
            console.error("Market data error", err);
            setError();
        }
    }

    rangeButtons.forEach(btn => {
        btn.addEventListener("click", () => {
            rangeButtons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");
            const range = btn.dataset.range;
            loadMarketData(range);
        });
    });

    loadMarketData("1D");
}

function initHome() {
    loadHeader();
    initLeagueActions();
    initHomeNavButtons();
    initHomeNotifications();
    initMarketChart();
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHome);
} else {
    initHome();
}
