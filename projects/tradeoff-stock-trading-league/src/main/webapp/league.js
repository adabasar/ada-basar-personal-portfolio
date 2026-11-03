let currentUserId = null;
let currentLeagueId = null;
let isAdmin = false;
let allRankings = [];

const loadingView = document.getElementById('loadingView');
const leagueView = document.getElementById('leagueView');
const noLeagueView = document.getElementById('noLeagueView');
const leagueNameDisplay = document.getElementById('leagueNameDisplay');
const rankingRows = document.getElementById('rankingRows');
const rosterList = document.getElementById('rosterList');
const settingsBtn = document.getElementById('settingsBtn');
const settingsPanel = document.getElementById('settingsPanel');
const cancelSettingsBtn = document.getElementById('cancelSettingsBtn');
const saveSettingsBtn = document.getElementById('saveSettingsBtn');
const settingsDangerZone = document.getElementById('settingsDangerZone');
const leaguesList = document.getElementById('leaguesList');

async function init() {
    try {
        const sessionResp = await fetch('user-auth?action=check-session');
        const sessionData = await sessionResp.json();

        if (!sessionData.loggedIn) {
            window.location.href = 'login.html';
            return;
        }

        currentUserId = sessionData.userID;
        const leagueResp = await fetch(`league?action=user-league&userId=${currentUserId}`);
        const leagueData = await leagueResp.json();

        loadingView.style.display = 'none';

		if (leagueData.success && leagueData.league) await showLeagueView(leagueData.league);
		else await showNoLeagueView();
    } catch (error) {
        console.error('Error initializing page:', error);
        loadingView.innerHTML = '<p style="color:#ef4444;">Error loading page. Please refresh.</p>';
    }
}

async function showLeagueView(league) {
    currentLeagueId = league.leagueId;
    isAdmin = league.adminUserId === currentUserId;

    leagueView.style.display = 'block';
    noLeagueView.style.display = 'none';

    leagueNameDisplay.textContent = league.leagueName;

    if (isAdmin && settingsBtn) settingsBtn.style.display = 'inline-flex';

    await loadRankings(league.leagueId);
    setupSettingsPanel(league);
}

async function showNoLeagueView() {
    leagueView.style.display = 'none';
    noLeagueView.style.display = 'block';

    await loadAvailableLeagues();
}

async function loadRankings(leagueId) {
    try {
        const resp = await fetch(`league?action=rankings&leagueId=${leagueId}`);
        const data = await resp.json();

        if (data.success && data.rankings) {
            allRankings = data.rankings;
            populateRankingsTable(data.rankings);
            populateRosterList(data.rankings);
        }
    } catch (error) {
        console.error('Error loading rankings:', error);
    }
}

function populateRankingsTable(rankings) {
    rankingRows.innerHTML = '';

    if (!rankings || rankings.length === 0) {
        rankingRows.innerHTML = '<tr><td colspan="5" style="text-align:center;color:#9ca3af;">No members yet</td></tr>';
        return;
    }

    rankings.forEach((member, index) => {
        const row = document.createElement('tr');
        row.dataset.username = member.username;
        row.dataset.totalPl = member.profitLoss;
        row.dataset.weeklyPl = member.weeklyProfitLoss;
        row.dataset.portfolioValue = member.portfolioValue;

        const initialCapital = 100000;
        const totalPlPercent = ((member.profitLoss / initialCapital) * 100).toFixed(2);
        const weeklyPlPercent = ((member.weeklyProfitLoss / initialCapital) * 100).toFixed(2);

        row.innerHTML = `
            <td>${index + 1}</td>
            <td><span class="clickable-name">${member.displayName || member.username}</span></td>
            <td style="color:${member.profitLoss >= 0 ? '#22c55e' : '#ef4444'}">${totalPlPercent}%</td>
            <td style="color:${member.weeklyProfitLoss >= 0 ? '#22c55e' : '#ef4444'}">${weeklyPlPercent}%</td>
            <td>$${member.portfolioValue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}</td>
        `;

        rankingRows.appendChild(row);
    });
}

function populateRosterList(rankings) {
    rosterList.innerHTML = '';

    if (!rankings || rankings.length === 0) {
        rosterList.innerHTML = '<li style="text-align:center;color:#9ca3af;">No members yet</li>';
        return;
    }

    rankings.forEach(member => {
        const li = document.createElement('li');
        li.dataset.username = member.username;

        const isLeagueAdmin = member.userId === (allRankings[0]?.adminUserId || 0); 
        
        li.innerHTML = `
            <span class="roster-name">${member.displayName || member.username}</span>
            ${member.userId === currentUserId ? '<span class="roster-role">(You)</span>' : ''}
        `;

        rosterList.appendChild(li);
    });
}

async function loadAvailableLeagues() {
    try {
        const resp = await fetch('league?action=list');
        const data = await resp.json();

        if (data.success && data.leagues) displayLeaguesList(data.leagues);
        
    } catch (error) {
        console.error('Error loading leagues:', error);
    }
}

function displayLeaguesList(leagues) {
    leaguesList.innerHTML = '';

    if (!leagues || leagues.length === 0) {
        leaguesList.innerHTML = '<p style="padding:10px;color:#9ca3af;text-align:center;">No public leagues available</p>';
        return;
    }

    const publicLeagues = leagues.filter(l => !l.isPrivate);

    if (publicLeagues.length === 0) {
        leaguesList.innerHTML = '<p style="padding:10px;color:#9ca3af;text-align:center;">No public leagues available</p>';
        return;
    }

    publicLeagues.forEach(league => {
        const row = document.createElement('div');
        row.className = 'league-row';
        row.dataset.leagueId = league.leagueId;

        row.innerHTML = `
            <div>
                <span class="league-name">${league.leagueName}</span>
                <span class="league-meta">${league.memberCount || 0} members</span>
            </div>
            <button class="btn btn-primary btn-join" data-league-id="${league.leagueId}">
                Join
            </button>
        `;

        leaguesList.appendChild(row);
    });
}

function setupSettingsPanel(league) {
    const leagueNameInput = document.getElementById('leagueNameInput');
    const isPrivateToggle = document.getElementById('isPrivateToggle');

    if (leagueNameInput) leagueNameInput.value = league.leagueName;
    if (isPrivateToggle) isPrivateToggle.checked = league.isPrivate;

    if (settingsDangerZone) {
        if (isAdmin) {
            settingsDangerZone.innerHTML = `
                <button type="button" class="btn-danger btn" id="deleteLeagueBtn">
                    Delete League
                </button>
            `;
        } else {
            settingsDangerZone.innerHTML = `
                <button type="button" class="btn btn-danger" id="leaveLeagueBtn">
                    Leave League
                </button>
            `;
        }
    }
}

document.addEventListener('click', async (e) => {
    const target = e.target;

    if (target.classList.contains('btn-join')) {
        const leagueId = parseInt(target.dataset.leagueId);
        await joinLeague(leagueId);
        return;
    }

    if (target.id === 'leaveLeagueBtn') {
        if (confirm('Are you sure you want to leave this league?')) await leaveLeague();
        
        return;
    }

    if (target.id === 'deleteLeagueBtn') {
        if (confirm('Delete this league for all members? This cannot be undone.')) await deleteLeague();
        
        return;
    }

    const clickableName = target.closest('.clickable-name') || target.closest('.roster-name');
    if (clickableName) {
        const row = clickableName.closest('tr') || clickableName.closest('li');
        const username = row?.dataset.username;
        if (username) window.location.href = `/profile?user=${encodeURIComponent(username)}`;
        
        return;
    }
});

if (settingsBtn) {
    settingsBtn.addEventListener('click', () => {
        settingsPanel.style.display = settingsPanel.style.display === 'block' ? 'none' : 'block';
    });
}

if (cancelSettingsBtn) {
    cancelSettingsBtn.addEventListener('click', () => {
        settingsPanel.style.display = 'none';
    });
}

if (saveSettingsBtn) {
    saveSettingsBtn.addEventListener('click', async () => {
        const leagueNameInput = document.getElementById('leagueNameInput');
        const isPrivateToggle = document.getElementById('isPrivateToggle');

        const newName = leagueNameInput.value.trim();
        const isPrivate = isPrivateToggle.checked;

        if (!newName) {
            alert('Please enter a league name');
            return;
        }

        try {
            const resp = await fetch('league', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    action: 'update',          
                    userId: currentUserId,
                    leagueId: currentLeagueId,
                    leagueName: newName,
                    isPrivate: isPrivate
                })
            });

            const data = await resp.json();

            if (data.success) {
                alert('League settings saved!');
                leagueNameDisplay.textContent = newName; 
                settingsPanel.style.display = 'none';
            } else {
                alert(data.message || 'Failed to update league settings');
            }
        } catch (err) {
            console.error('Error updating league:', err);
            alert('Error updating league settings');
        }
    });
}


document.getElementById('createLeagueBtn')?.addEventListener('click', async () => {
    const leagueName = document.getElementById('newLeagueName').value.trim();
    const isPrivate = document.getElementById('newLeaguePrivate').checked;

    if (!leagueName) {
        alert('Please enter a league name');
        return;
    }

    await createLeague(leagueName, isPrivate);
});

document.getElementById('sortProfitLoss')?.addEventListener('click', () => {
    setActiveSortButton(document.getElementById('sortProfitLoss'));
    sortBy('totalPl');
});

document.getElementById('sortWeeklyPL')?.addEventListener('click', () => {
    setActiveSortButton(document.getElementById('sortWeeklyPL'));
    sortBy('weeklyPl');
});

document.getElementById('sortPortfolioValue')?.addEventListener('click', () => {
    setActiveSortButton(document.getElementById('sortPortfolioValue'));
    sortBy('portfolioValue');
});

function setActiveSortButton(activeBtn) {
    document.querySelectorAll('.btn-pill').forEach(btn => btn.classList.remove('active'));
    if (activeBtn) activeBtn.classList.add('active');
}

function sortBy(attrName) {
    const rows = Array.from(rankingRows.querySelectorAll('tr'));
    rows.sort((a, b) => {
        const aval = parseFloat(a.dataset[attrName]) || 0;
        const bval = parseFloat(b.dataset[attrName]) || 0;
        return bval - aval;
    });
    rows.forEach((row, index) => {
        const rankCell = row.querySelector('td:first-child');
        if (rankCell) rankCell.textContent = index + 1;
        rankingRows.appendChild(row);
    });
}

async function joinLeague(leagueId) {
    try {
        const resp = await fetch('league', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'join',
                userId: currentUserId,
                leagueId: leagueId
            })
        });

        const data = await resp.json();

        if (data.success) {
            alert('Successfully joined league!');
            location.reload();
        } else alert(data.message || 'Failed to join league');
        
    } catch (error) {
        console.error('Error joining league:', error);
        alert('Error joining league');
    }
}

async function leaveLeague() {
    try {
        const resp = await fetch('league', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'leave',
                userId: currentUserId,
                leagueId: currentLeagueId
            })
        });

        const data = await resp.json();

        if (data.success) {
            alert('Successfully left league');
            location.reload();
        } else {
            alert(data.message || 'Failed to leave league');
        }
    } catch (error) {
        console.error('Error leaving league:', error);
        alert('Error leaving league');
    }
}

async function deleteLeague() {
    try {
        const resp = await fetch('league', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'delete',
                userId: currentUserId,
                leagueId: currentLeagueId
            })
        });

        const data = await resp.json();

        if (data.success) {
            alert('League deleted successfully');
            location.reload();
        } else {
            alert(data.message || 'Failed to delete league');
        }
    } catch (error) {
        console.error('Error deleting league:', error);
        alert('Error deleting league');
    }
}

async function createLeague(leagueName, isPrivate) {
    try {
        const resp = await fetch('league', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                action: 'create',
                userId: currentUserId,
                leagueName: leagueName,
                isPrivate: isPrivate
            })
        });

        const data = await resp.json();

        if (data.success) {
            alert('League created successfully!');
            location.reload();
        } else {
            alert(data.message || 'Failed to create league');
        }
    } catch (error) {
        console.error('Error creating league:', error);
        alert('Error creating league');
    }
}

function loadContent(placeholderId, filePath) {
    return fetch(filePath)
        .then(r => r.text())
        .then(html => {
            document.getElementById(placeholderId).innerHTML = html;
        });
}

function hideLeagueButton() {
    const leagueButton = document.querySelector('.nav-league');
    if (leagueButton) {
        leagueButton.style.display = 'none';
    }
}

loadContent('header-placeholder', 'header.html')
    .then(() => {
        hideLeagueButton();
        if (typeof initHeader === 'function') {
            initHeader();
        }
        init();
    });

window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        init(); 
    }
});
