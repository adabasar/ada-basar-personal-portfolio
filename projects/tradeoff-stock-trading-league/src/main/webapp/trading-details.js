const API_TOP_STOCKS_URL = "/stocks?action=top10";      
const API_STOCK_SEARCH_URL = "/stocks?action=search";   
const API_WATCHLIST_URL = "/watchlist";                      

let allTopStocks = [];
let filteredStocks = [];
let watchlist = [];

// Centralized metrics provider (swap implementation later without touching UI code)
async function getStockFundamentals(symbol) {
  const key = (symbol || "").toUpperCase();

const fundamentalsBySymbol = {
  AAPL: {
    marketCap: 3.04e12,
    peRatio: 32.18,
    dividendYieldPct: 0.55,
    beta: 1.20,
    rating: "Buy / A",
    dayLow: 193.10,
    dayHigh: 196.80
  },
  TSLA: {
    marketCap: 7.60e11,
    peRatio: 62.40,
    dividendYieldPct: 0.00,
    beta: 2.02,
    rating: "Hold / B",
    dayLow: 240.10,
    dayHigh: 250.40
  },
  NVDA: {
    marketCap: 2.20e12,
    peRatio: 58.90,
    dividendYieldPct: 0.03,
    beta: 1.65,
    rating: "Buy / A",
    dayLow: 715.00,
    dayHigh: 745.00
  },
  AMZN: {
    marketCap: 1.65e12,
    peRatio: 52.10,
    dividendYieldPct: 0.00,
    beta: 1.25,
    rating: "Buy / A-",
    dayLow: 158.90,
    dayHigh: 162.40
  },
  META: {
    marketCap: 1.05e12,
    peRatio: 28.70,
    dividendYieldPct: 0.00,
    beta: 1.30,
    rating: "Buy / A-",
    dayLow: 404.20,
    dayHigh: 417.10
  },
  MSFT: {
    marketCap: 3.10e12,
    peRatio: 36.40,
    dividendYieldPct: 0.72,
    beta: 0.92,
    rating: "Buy / A",
    dayLow: 416.10,
    dayHigh: 424.90
  },
  GOOGL: {
    marketCap: 1.90e12,
    peRatio: 24.60,
    dividendYieldPct: 0.00,
    beta: 1.05,
    rating: "Buy / A-",
    dayLow: 148.60,
    dayHigh: 151.90
  },
  NFLX: {
    marketCap: 2.25e11,
    peRatio: 44.20,
    dividendYieldPct: 0.00,
    beta: 1.35,
    rating: "Hold / B+",
    dayLow: 502.10,
    dayHigh: 517.60
  },
  JPM: {
    marketCap: 4.75e11,
    peRatio: 11.90,
    dividendYieldPct: 2.35,
    beta: 1.10,
    rating: "Buy / A-",
    dayLow: 163.40,
    dayHigh: 166.30
  },
  BAC: {
    marketCap: 3.05e11,
    peRatio: 10.30,
    dividendYieldPct: 2.60,
    beta: 1.25,
    rating: "Hold / B",
    dayLow: 38.20,
    dayHigh: 39.10
  }
};


  // fallback: return reasonable defaults
  return fundamentalsBySymbol[key] || {
    marketCap: null,
    peRatio: null,
    dividendYieldPct: null,
    beta: null,
    rating: null,
    dayLow: null,
    dayHigh: null
  };
}

function formatMarketCap(val) {
  if (val == null) return "—";
  if (val >= 1e12) return `$${(val / 1e12).toFixed(2)}T`;
  if (val >= 1e9) return `$${(val / 1e9).toFixed(2)}B`;
  if (val >= 1e6) return `$${(val / 1e6).toFixed(2)}M`;
  return `$${Number(val).toLocaleString()}`;
}

function $(selector) {
    return document.querySelector(selector);
}

function createElement(tag, className, html) {
    const el = document.createElement(tag);
    if (className) el.className = className;
    if (html !== undefined) el.innerHTML = html;
    return el;
}

function emitStockSelected(stock) {
    const event = new CustomEvent("stockSelected", { detail: stock });
    document.dispatchEvent(event);
}

function renderTopStocks() {
    const container = $("#topStocksList");
    if (!container) return;

    container.innerHTML = "";

    if (!filteredStocks.length) {
        container.textContent = "No stocks match your search.";
        return;
    }

    filteredStocks.forEach((stock) => {
        const inWatchlist = watchlist.some(w => w.symbol === stock.symbol);

        const row = createElement("div", "stock-row");
        row.dataset.symbol = stock.symbol;

        row.innerHTML = `
            <div class="stock-main">
                <div class="stock-symbol">${stock.symbol}</div>
                <div class="stock-name">${stock.name}</div>
                <div class="stock-meta">
                    <span class="stock-sector">${stock.sector || stock.type || ""}</span>
                    ${stock.areaOfInterest ? `<span class="stock-tag">${stock.areaOfInterest}</span>` : ""}
                </div>
            </div>
            <div class="stock-pricing">
                <div class="stock-price">$${stock.currentPrice.toFixed(2)}</div>
                <div class="stock-change ${stock.changePct >= 0 ? "positive" : "negative"}">
                    ${stock.changePct >= 0 ? "+" : ""}${stock.changePct.toFixed(2)}%
                </div>
                <div class="stock-earnings">
                    Day P&L: 
                    <span class="${stock.earningsToday >= 0 ? "positive" : "negative"}">
                        ${stock.earningsToday >= 0 ? "+" : ""}$${stock.earningsToday.toFixed(2)}
                    </span>
                </div>
            </div>
            <button class="watchlist-toggle ${inWatchlist ? "active" : ""}" 
                    title="${inWatchlist ? "Remove from watchlist" : "Add to watchlist"}"
                    data-symbol="${stock.symbol}">
                ★
            </button>
        `;

        container.appendChild(row);
    });
}

function renderWatchlist() {
    const container = $("#watchlistList");
    if (!container) return;

    container.innerHTML = "";

    if (!watchlist.length) {
        container.textContent = "Your watchlist is empty. Add stocks using the star icon.";
        return;
    }

    watchlist.forEach((stock) => {
        const row = createElement("div", "watchlist-row");
        row.dataset.symbol = stock.symbol;

        row.innerHTML = `
            <div class="watchlist-main">
                <div class="watchlist-symbol">${stock.symbol}</div>
                <div class="watchlist-name">${stock.name}</div>
            </div>
            <div class="watchlist-pricing">
                <span class="watchlist-price">$${stock.currentPrice.toFixed(2)}</span>
                <span class="watchlist-change ${stock.changePct >= 0 ? "positive" : "negative"}">
                    ${stock.changePct >= 0 ? "+" : ""}${stock.changePct.toFixed(2)}%
                </span>
            </div>
            <button class="watchlist-remove-btn" data-symbol="${stock.symbol}">
                ✕
            </button>
        `;
        container.appendChild(row);
    });
}

function filterStocks(query) {
    if (!query) filteredStocks = [...allTopStocks];
    else {
        const q = query.toLowerCase();
        filteredStocks = allTopStocks.filter((stock) => {
            return (
                stock.name.toLowerCase().includes(q) ||
                stock.symbol.toLowerCase().includes(q) ||
                (stock.sector && stock.sector.toLowerCase().includes(q)) ||
                (stock.type && stock.type.toLowerCase().includes(q)) ||
                (stock.areaOfInterest && stock.areaOfInterest.toLowerCase().includes(q))
            );
        });
    }
    renderTopStocks();
}

async function addToWatchlist(stock) {
    try {
        await fetch(API_WATCHLIST_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                action: "add",
                symbol: stock.symbol
            })
        });
    } catch (err) {
        console.error("Failed to update watchlist.", err);
    }
}

async function removeFromWatchlist(symbol) {
    try {
        await fetch(API_WATCHLIST_URL, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                action: "remove",
                symbol
            })
        });
    } catch (err) {
        console.error("Failed to update watchlist.", err);
    }
}

function toggleWatchlistForSymbol(symbol) {
    const existing = watchlist.find(s => s.symbol === symbol);
    if (existing) {
        watchlist = watchlist.filter(s => s.symbol !== symbol);
        removeFromWatchlist(symbol);
    } else {
        const fromTop = allTopStocks.find(s => s.symbol === symbol);
        const newItem = fromTop || { symbol, name: symbol, currentPrice: 0, changePct: 0 };
        watchlist.push(newItem);
        addToWatchlist(newItem);
    }
    renderTopStocks();
    renderWatchlist();
}

async function loadTopStocks() {
    try {
        const res = await fetch(API_TOP_STOCKS_URL);
        if (!res.ok) throw new Error("Failed to load top stocks");

        const data = await res.json();
        allTopStocks = data.stocks || data || [];
    } catch (err) {
        console.error("Top stocks API failed, using fallback mock data.", err);
        allTopStocks = [
            { symbol: "AAPL", name: "Apple Inc.", currentPrice: 195.23, changePct: 1.35, earningsToday: 120.50, sector: "Tech" },
            { symbol: "TSLA", name: "Tesla Inc.", currentPrice: 245.80, changePct: -0.85, earningsToday: -56.10, sector: "Auto" },
            { symbol: "NVDA", name: "NVIDIA Corp.", currentPrice: 730.12, changePct: 2.45, earningsToday: 300.00, sector: "Tech" },
            { symbol: "AMZN", name: "Amazon.com Inc.", currentPrice: 160.45, changePct: 0.75, earningsToday: 78.23, sector: "E-Commerce" },
            { symbol: "META", name: "Meta Platforms", currentPrice: 410.17, changePct: -1.10, earningsToday: -34.80, sector: "Tech" },
            { symbol: "MSFT", name: "Microsoft Corp.", currentPrice: 420.33, changePct: 0.95, earningsToday: 102.11, sector: "Tech" },
            { symbol: "GOOGL", name: "Alphabet Inc.", currentPrice: 150.27, changePct: 0.67, earningsToday: 40.05, sector: "Tech" },
            { symbol: "NFLX", name: "Netflix Inc.", currentPrice: 510.80, changePct: 1.90, earningsToday: 90.45, sector: "Media" },
            { symbol: "JPM", name: "JPMorgan Chase", currentPrice: 165.10, changePct: 0.40, earningsToday: 25.15, sector: "Finance" },
            { symbol: "BAC", name: "Bank of America", currentPrice: 38.73, changePct: -0.30, earningsToday: -5.25, sector: "Finance" }
        ];
    }
    filteredStocks = [...allTopStocks];
    renderTopStocks();
}

async function loadWatchlist() {
    try {
        const res = await fetch(API_WATCHLIST_URL);
        if (!res.ok) throw new Error("Failed to load watchlist");

        const data = await res.json();
        watchlist = data.watchlist || [];
    } catch (err) {
        console.error("API failed, so using empty watchlist.", err);
        watchlist = [];
    }
    renderWatchlist();
}

function initTradingLeftSide() {
    const searchInput = $("#stockSearchInput");
    const topStocksContainer = $("#topStocksList");
    const watchlistContainer = $("#watchlistList");

    if (searchInput) {
        searchInput.addEventListener("input", (e) => {
            filterStocks(e.target.value);
        });
    }

    if (topStocksContainer) {
        topStocksContainer.addEventListener("click", (e) => {
            const watchBtn = e.target.closest(".watchlist-toggle");
            const row = e.target.closest(".stock-row");

            if (!row) return;
            const symbol = row.dataset.symbol;
            const stockObj = allTopStocks.find(s => s.symbol === symbol) || { symbol };

            if (watchBtn) toggleWatchlistForSymbol(symbol);
            else emitStockSelected(stockObj);
        });
    }
	
    if (watchlistContainer) {
        watchlistContainer.addEventListener("click", (e) => {
            const removeBtn = e.target.closest(".watchlist-remove-btn");
            const row = e.target.closest(".watchlist-row");

            if (!row) return;
            const symbol = row.dataset.symbol;
            const stockObj = watchlist.find(s => s.symbol === symbol) || { symbol };

            if (removeBtn) toggleWatchlistForSymbol(symbol);
            else emitStockSelected(stockObj);
        });
    }
    loadTopStocks();
    loadWatchlist();
}

if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initTradingLeftSide);
else  initTradingLeftSide();

(function () {
  const symbolEl = document.getElementById("details-symbol");
  const priceEl = document.getElementById("details-current-price");
  const changeEl = document.getElementById("details-change");
  const marketCapEl = document.getElementById("metric-marketcap");
  const peEl = document.getElementById("metric-pe");
  const divEl = document.getElementById("metric-dividend");
  const currentEl = document.getElementById("metric-current");
  const lowHighEl = document.getElementById("metric-lowhigh");
  const betaEl = document.getElementById("metric-beta");
  const ratingEl = document.getElementById("metric-rating");
  const aboutEl = document.getElementById("about-description");
  const rangeButtons = document.querySelectorAll(".trend-btn");
  const chartCanvas = document.getElementById("stock-chart");
  const chartLoading = document.getElementById("chart-loading");
  const chartError = document.getElementById("chart-error");
  let chartInstance = null;

  if (!symbolEl) {
    console.warn("Right-side panel not found on this page.");
    return;
  }

  let currentStock = null;
  let currentRange = "1D";

  function formatMoney(val) {
    if (val == null || isNaN(val)) return "$--";
    return "$" + Number(val).toFixed(2);
  }

  function setChangeBadge(percentage) {
    if (!changeEl) return;

    changeEl.classList.remove("positive", "negative", "neutral");
    const pct = Number(percentage || 0);

    if (pct > 0) changeEl.classList.add("positive");
    else if (pct < 0) changeEl.classList.add("negative");
    else changeEl.classList.add("neutral");

    const sign = pct > 0 ? "+" : "";
    changeEl.textContent = `${sign}${pct.toFixed(2)}%`;
  }

  function setActiveRangeButton(range) {
    rangeButtons.forEach((btn) => {
      if (btn.dataset.range === range) btn.classList.add("active");
      else btn.classList.remove("active");
    });
  }

  function setChartLoading(isLoading) {
    if (!chartLoading || !chartError) return;
    chartLoading.classList.toggle("hidden", !isLoading);
    chartError.classList.add("hidden");
  }

  function showChartError(msg) {
    if (!chartError || !chartLoading) return;
    chartLoading.classList.add("hidden");
    chartError.textContent = msg || "Could not load historical data.";
    chartError.classList.remove("hidden");
  }

  function renderChart(labels, values) {
    if (!chartCanvas || typeof Chart === "undefined") {
      console.warn("Chart.js or canvas not available.");
      return;
    }

    const ctx = chartCanvas.getContext("2d");
    if (!ctx) return;

    if (chartInstance)  chartInstance.destroy();

    chartInstance = new Chart(ctx, {
      type: "line",
      data: {
        labels,
        datasets: [
          {
            label: "Price",
            data: values,
            borderWidth: 2,
            fill: false,
            tension: 0.2
          }
        ]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: {
            ticks: { maxTicksLimit: 6 }
          }
        }
      }
    });
  }

  function generateMockHistory(stock, range) {
    const base = Number(stock.currentPrice) || 100;
    let points = 20;
    let labelPrefix = "";

    switch (range) {
      case "1D":
        points = 12;
        labelPrefix = "T";
        break;
      case "1W":
        points = 7;
        labelPrefix = "D";
        break;
      case "1M":
        points = 30;
        labelPrefix = "D";
        break;
      case "6M":
        points = 24;
        labelPrefix = "W";
        break;
      case "1Y":
        points = 12;
        labelPrefix = "M";
        break;
      case "5Y":
        points = 10;
        labelPrefix = "Y";
        break;
    }


    const labels = [];
    const values = [];
    let price = base;

    for (let i = 0; i < points; i++) {
      labels.push(`${labelPrefix}${i + 1}`);
      const delta = (Math.random() - 0.5) * (base * 0.01);
      price = Math.max(1, price + delta);
      values.push(Number(price.toFixed(2)));
    }

    return { labels, values };
  }

  function updateTrendForRange() {
    if (!currentStock) return;
    if (!chartCanvas) return;

    setChartLoading(true);

    try {
      const { labels, values } = generateMockHistory(currentStock, currentRange);
      renderChart(labels, values);
      setChartLoading(false);
    } catch (err) {
      console.error("Error updating mock chart:", err);
      showChartError();
    }
  }

async function renderDetails(stock) {
  const symbol = stock.symbol;

  symbolEl.textContent = `Details for ${symbol}`;
  priceEl.textContent = formatMoney(stock.currentPrice);
  currentEl.textContent = formatMoney(stock.currentPrice);
  setChangeBadge(stock.percentChange);

  const f = await getStockFundamentals(symbol);

  marketCapEl.textContent = formatMarketCap(f.marketCap);
  peEl.textContent = (f.peRatio != null) ? Number(f.peRatio).toFixed(2) : "—";
  divEl.textContent = (f.dividendYieldPct != null) ? `${Number(f.dividendYieldPct).toFixed(2)}%` : "—";
  betaEl.textContent = (f.beta != null) ? Number(f.beta).toFixed(2) : "—";
  ratingEl.textContent = f.rating || "—";

  const low = (stock.todayLow != null) ? stock.todayLow : f.dayLow;
  const high = (stock.todayHigh != null) ? stock.todayHigh : f.dayHigh;
  lowHighEl.textContent = (low != null && high != null) ? `${formatMoney(low)} / ${formatMoney(high)}` : "—";

  aboutEl.textContent = stock.about || `You selected ${symbol}.`;

  currentStock = stock;
  updateTrendForRange();
}

  document.addEventListener("stockSelected", (e) => {
    const stock = e.detail;
    if (stock) renderDetails(stock);
  });

  rangeButtons.forEach((btn) => {
    btn.addEventListener("click", () => {
      const range = btn.dataset.range;
      if (!range) return;

      currentRange = range;
      setActiveRangeButton(range);
      updateTrendForRange();
    });
  });

  setActiveRangeButton(currentRange);
})();
