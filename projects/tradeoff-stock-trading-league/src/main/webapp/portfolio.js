// Load portfolio data when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadPortfolioData();
    initChartRangeButtons();
});

let portfolioChart = null; // Store chart instance globally

function loadPortfolioData() {
    // Show loading state
    showLoading();
    
    // Call the servlet
    fetch('/TradeOff/api/portfolio')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            console.log('Portfolio data received:', data);
            if (data.success) {
                updatePortfolioUI(data);
                initPortfolioChart(data);
            } else {
                console.error('Error:', data.message);
                showError(data.message);
            }
        })
        .catch(error => {
            console.error('Fetch error:', error);
            showError('Failed to load portfolio data. Please log in first.');
        });
}

function updatePortfolioUI(data) {
    // Update available capital
    const balanceEl = document.querySelector('.balance');
    if (balanceEl) {
        balanceEl.textContent = '$' + formatCurrency(data.availableCapital);
        balanceEl.style.color = '#f9fafb'; // Reset color in case of previous error
    }
    
    // Update total portfolio value (first stat item)
    const portfolioValueEl = document.querySelector('.stat-value');
    if (portfolioValueEl) {
        portfolioValueEl.textContent = '$' + formatCurrency(data.totalValue);
    }
    
    // Update profit/loss (second stat item)
    const statValues = document.querySelectorAll('.stat-value');
    if (statValues.length > 1) {
        const plEl = statValues[1];
        const plValue = data.profitLoss || 0;
        const plPercent = data.percentChange || 0;
        const sign = plValue >= 0 ? '+' : '';
        
        plEl.textContent = sign + '$' + formatCurrency(Math.abs(plValue)) + 
                          ' (' + sign + plPercent.toFixed(1) + '%)';
        
        // Remove previous classes
        plEl.classList.remove('positive', 'negative');
        plEl.classList.add(plValue >= 0 ? 'positive' : 'negative');
    }
    
    // Update holdings table
    updateHoldingsTable(data.holdings || []);
    
    // Update watchlist
    updateWatchlist(data.watchlist || []);
    
    // Hide loading
    hideLoading();
}

function updateHoldingsTable(holdings) {
    const tbody = document.getElementById('investmentsBody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    if (holdings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#9ca3af; padding: 20px;">No holdings yet. Start trading to build your portfolio!</td></tr>';
        return;
    }
    
    holdings.forEach(holding => {
        const row = document.createElement('tr');
        
        const gainLoss = holding.gainLoss || 0;
        const gainLossPercent = holding.gainLossPercent || 0;
        const isPositive = gainLoss >= 0;
        
        row.innerHTML = `
            <td class="holding-name">${holding.ticker}</td>
            <td>${holding.quantity}</td>
            <td class="${isPositive ? 'price-positive' : 'price-negative'}">
                ${isPositive ? '+' : ''}$${Math.abs(gainLoss).toFixed(2)}
            </td>
            <td class="${isPositive ? 'percent-positive' : 'percent-negative'}">
                ${isPositive ? '+' : ''}${gainLossPercent.toFixed(1)}%
            </td>
        `;
        
        tbody.appendChild(row);
    });
}

function updateWatchlist(watchlist) {
    const grid = document.getElementById('watchlistGrid');
    if (!grid) return;
    
    grid.innerHTML = '';
    
    if (watchlist.length === 0) {
        grid.innerHTML = '<p style="color:#9ca3af; grid-column: 1/-1; padding: 20px; text-align: center;">No stocks in watchlist. Add stocks to track their performance!</p>';
        return;
    }
    
    watchlist.forEach(stock => {
        const item = document.createElement('div');
        item.className = 'watchlist-item';
        
        const changePercent = stock.changePercent || 0;
        const isPositive = changePercent >= 0;
        const currentPrice = stock.currentPrice || 0;
        
        item.innerHTML = `
            <div class="watchlist-symbol">${stock.ticker}</div>
            <div class="watchlist-name">${stock.companyName}</div>
            <div class="watchlist-price">$${currentPrice.toFixed(2)}</div>
            <div class="watchlist-change ${isPositive ? 'positive' : 'negative'}">
                ${isPositive ? '+' : ''}${changePercent.toFixed(1)}%
            </div>
        `;
        
        grid.appendChild(item);
    });
}

function initPortfolioChart(data) {
    const ctx = document.getElementById('portfolioChart');
    if (!ctx) {
        console.error('Portfolio chart canvas not found');
        return;
    }

    const chartLoadingEl = document.getElementById('chartLoading');
    
    // Generate mock historical data based on current portfolio value
    const historicalData = generateHistoricalData(data.totalValue, data.profitLoss);
    
    // Destroy existing chart if it exists
    if (portfolioChart) {
        portfolioChart.destroy();
    }

    portfolioChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: historicalData.labels,
            datasets: [{
                label: 'Portfolio Value',
                data: historicalData.values,
                borderColor: data.profitLoss >= 0 ? '#22c55e' : '#f97373',
                backgroundColor: data.profitLoss >= 0 ? 'rgba(34, 197, 94, 0.1)' : 'rgba(249, 115, 115, 0.1)',
                tension: 0.4,
                fill: true,
                pointRadius: 3,
                pointHoverRadius: 6,
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: '#1f2937',
                    titleColor: '#f9fafb',
                    bodyColor: '#e5e7eb',
                    borderColor: '#374151',
                    borderWidth: 1,
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        label: function(context) {
                            return 'Value: $' + formatCurrency(context.parsed.y);
                        }
                    }
                }
            },
            scales: {
                y: {
                    ticks: { 
                        color: '#9ca3af', 
                        font: { size: 11 },
                        callback: function(value) {
                            return '$' + (value / 1000).toFixed(0) + 'k';
                        }
                    },
                    grid: { 
                        color: '#1f2937',
                        drawBorder: false
                    },
                    beginAtZero: false
                },
                x: {
                    ticks: { 
                        color: '#9ca3af', 
                        font: { size: 11 },
                        maxRotation: 0
                    },
                    grid: { 
                        display: false 
                    }
                }
            },
            interaction: {
                intersect: false,
                mode: 'index'
            }
        }
    });

    if (chartLoadingEl) {
        chartLoadingEl.style.display = 'none';
    }
}

function generateHistoricalData(currentValue, profitLoss) {
    const initialCapital = 100000; // Match the backend default
    const dataPoints = 12; // 12 months
    const labels = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    
    // Calculate the starting value (current value minus profit/loss)
    const startValue = currentValue - profitLoss;
    
    // Generate a realistic growth curve
    const values = [];
    for (let i = 0; i < dataPoints; i++) {
        const progress = i / (dataPoints - 1); // 0 to 1
        
        // Add some randomness for realism
        const randomness = (Math.random() - 0.5) * 0.05; // ±5% variation
        
        // Linear interpolation with some curve
        let value = startValue + (profitLoss * progress) + (currentValue * randomness);
        
        // Make sure values don't go below a reasonable minimum
        value = Math.max(value, initialCapital * 0.7);
        
        values.push(Math.round(value * 100) / 100); // Round to 2 decimals
    }
    
    // Ensure the last value matches current value
    values[dataPoints - 1] = currentValue;
    
    return {
        labels: labels,
        values: values
    };
}

function initChartRangeButtons() {
    const buttons = document.querySelectorAll('#chartRangeGroup .btn-pill');
    buttons.forEach(btn => {
        btn.addEventListener('click', function() {
            buttons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            const range = this.getAttribute('data-range');
            updateChartRange(range);
        });
    });
}

function updateChartRange(range) {
    // TODO: Fetch data based on selected range
    // For now, just log the selection
    console.log('Chart range changed to:', range);
    
    // You can reload portfolio data with range parameter
    // fetch('/TradeOff/api/portfolio?range=' + range)
    //     .then(...)
}

function formatCurrency(value) {
    return value.toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function showLoading() {
    const loadingEl = document.getElementById('chartLoading');
    if (loadingEl) {
        loadingEl.style.display = 'flex';
        loadingEl.textContent = 'Loading portfolio...';
        loadingEl.classList.remove('chart-status-error');
    }
}

function hideLoading() {
    const loadingEl = document.getElementById('chartLoading');
    if (loadingEl) {
        loadingEl.style.display = 'none';
    }
}

function showError(message) {
    console.error(message);
    
    const loadingEl = document.getElementById('chartLoading');
    if (loadingEl) {
        loadingEl.style.display = 'flex';
        loadingEl.textContent = message;
        loadingEl.classList.add('chart-status-error');
    }
    
    // Show error in balance area too
    const balanceEl = document.querySelector('.balance');
    if (balanceEl) {
        balanceEl.textContent = 'Error';
        balanceEl.style.color = '#f97373';
    }
}

// Refresh portfolio data every 60 seconds
setInterval(function() {
    loadPortfolioData();
}, 60000);