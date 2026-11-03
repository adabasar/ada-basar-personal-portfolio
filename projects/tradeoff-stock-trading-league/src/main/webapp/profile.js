let currentUserId = null;
let sessionUsername = null;

/* DOM Elements */
const usernameDisplay = document.getElementById("username");
const displayNameInput = document.getElementById("displayName");
const contactInfoInput = document.getElementById("contactInfo");
const visibilityToggle = document.getElementById("profileVisibility");
const personalInfoMsg = document.getElementById("personalInfoMsg");

const currentLeagueField = document.getElementById("currentLeague");
const leagueRankingField = document.getElementById("leagueRanking");
const performanceField = document.getElementById("performance");
const availableCapitalField = document.getElementById("availableCapital");
const portfolioValueField = document.getElementById("portfolioValue");

/* ---------------------------------------------------------
   Fetch league data from LeagueServlet
--------------------------------------------------------- */
async function loadLeagueInfo(userId) {
    try {
        const res = await fetch(`league?action=user-league&userId=${encodeURIComponent(userId)}`);
        const data = await res.json();

        if (data.success && data.league) {
            const league = data.league;
            
            // Set league name
            currentLeagueField.textContent = league.leagueName || "-";
            
            // Set league ranking
            leagueRankingField.textContent = league.userRank ? `#${league.userRank}` : "-";
        } else {
            // User is not in a league
            currentLeagueField.textContent = "-";
            leagueRankingField.textContent = "-";
        }

    } catch (err) {
        console.error("Error fetching league info:", err);
        currentLeagueField.textContent = "-";
        leagueRankingField.textContent = "-";
    }
}

/* ---------------------------------------------------------
   Fetch portfolio profit/loss from PortfolioServlet
--------------------------------------------------------- */
async function loadPortfolioProfitLoss() {
    try {
        const res = await fetch("api/portfolio");
        const data = await res.json();

        if (data.success) {
            // Portfolio Value
            const portfolioValue = data.portfolioValue ?? 0;
            portfolioValueField.textContent = `${portfolioValue.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
            
            // Profit/Loss
            const profitLoss = data.profitLoss ?? 0;
            const profitLossFormatted = profitLoss >= 0 ? 
                `+${profitLoss.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}` : 
                `-${Math.abs(profitLoss).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
            
            performanceField.textContent = profitLossFormatted;
            performanceField.style.color = profitLoss >= 0 ? '#22c55e' : '#ef4444';
        } else {
            portfolioValueField.textContent = "-";
            performanceField.textContent = "-";
        }

    } catch (err) {
        console.error("Error fetching portfolio profit/loss:", err);
        portfolioValueField.textContent = "-";
        performanceField.textContent = "-";
    }
}

/* ---------------------------------------------------------
   INIT — Called after header loads
--------------------------------------------------------- */
async function initProfilePage() {
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

        // 2. Fetch user profile
        await loadProfile(currentUserId);

        // 3. Fetch league information
        await loadLeagueInfo(currentUserId);

        // 4. Fetch portfolio profit/loss
        await loadPortfolioProfitLoss();

    } catch (err) {
        console.error("Error initializing profile page:", err);
    }
}

/* ---------------------------------------------------------
   Fetch profile data from ProfileServlet
--------------------------------------------------------- */
async function loadProfile(userId) {
    try {
        const res = await fetch(`profile?userId=${encodeURIComponent(userId)}`);
        const data = await res.json();

        if (!data.success) {
            console.error("Profile error:", data.message);
            return;
        }

        const p = data.profile;
		
        if (!p) {
            console.error("Profile data is missing");
            return;
        }
			
        // Basic Info
        usernameDisplay.textContent = p.username ?? "Unknown User";
        displayNameInput.value = p.displayName ?? "";
        contactInfoInput.value = p.contactInfo ?? "";
        visibilityToggle.checked = p.profileVisibility === true;

        // Available Capital
        const availableCapital = p.availableCapital ?? 0;
        availableCapitalField.textContent = `${availableCapital.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;

    } catch (err) {
        console.error("Error fetching profile:", err);
        usernameDisplay.textContent = "Error loading profile";
    }
}

/* ---------------------------------------------------------
   Save Profile Changes
--------------------------------------------------------- */
async function saveProfile() {
    const payload = {
        action: "update",
        userId: currentUserId,
        displayName: displayNameInput.value.trim(),
        contactInfo: contactInfoInput.value.trim(),
        profileVisibility: visibilityToggle.checked
    };

    try {
        const res = await fetch("profile", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (data.success) {
            personalInfoMsg.className = "success-msg";
            personalInfoMsg.textContent = "Profile updated successfully!";
        } else {
            personalInfoMsg.className = "error-msg";
            personalInfoMsg.textContent =
                "Error updating profile: " + (data.message || "Unknown error");
        }

    } catch (err) {
        console.error("Error saving profile:", err);
        personalInfoMsg.className = "error-msg";
        personalInfoMsg.textContent = "Error saving profile";
    }
}

/* ---------------------------------------------------------
   Load Header → then Init Profile Page
--------------------------------------------------------- */
function loadContent(placeholderId, filePath) {
    return fetch(filePath)
        .then(r => r.text())
        .then(html => {
            document.getElementById(placeholderId).innerHTML = html;
        });
}

/* Load header and initialize profile */
loadContent("header-placeholder", "header.html")
    .then(() => {
        if (typeof initHeader === "function") initHeader();
        initProfilePage();
    })
    .then(() => {
        // Initialize friends modal after everything else loads
        console.log("Initializing friends modal...");
        setTimeout(initFriendsModal, 1000);
    });

/* Handle page restore from back/forward cache */
window.addEventListener("pageshow", (e) => {
    if (e.persisted) {
        initProfilePage();
        setTimeout(initFriendsModal, 500);
    }
});

/* Save button click */
document.getElementById("saveProfileBtn")?.addEventListener("click", saveProfile);
function initFriendsModal() {
    console.log("=== Initializing Friends Modal ===");
    
    const friendsModal = document.getElementById("friendsModal");
    const friendsListBtn = document.getElementById("friendsListBtn");
    const closeModalBtn = document.getElementById("closeModalBtn");
    const friendsTab = document.getElementById("friendsTab");
    const pendingTab = document.getElementById("pendingTab");
    const addFriendTab = document.getElementById("addFriendTab");
    const friendsView = document.getElementById("friendsView");
    const pendingView = document.getElementById("pendingView");
    const addFriendView = document.getElementById("addFriendView");
    const friendsList = document.getElementById("friendsList");
    const pendingList = document.getElementById("pendingList");
    const friendSearchInput = document.getElementById("friendSearchInput");
    const searchResults = document.getElementById("searchResults");

    console.log("friendsModal:", friendsModal);
    console.log("friendsListBtn:", friendsListBtn);
    console.log("closeModalBtn:", closeModalBtn);
    
    if (!friendsModal) {
        console.error("ERROR: friendsModal not found!");
        return;
    }
    
    if (!friendsListBtn) {
        console.error("ERROR: friendsListBtn not found!");
        return;
    }

    let searchTimeout = null;

    // Open modal
    console.log("Attaching click handler to friends button");
    friendsListBtn.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        console.log("=== Friends list button clicked ===");
        console.log("currentUserId:", currentUserId);
        friendsModal.style.display = "flex";
        console.log("Modal display set to flex");
        loadFriendsList();
    });
    
    console.log("Click handler attached successfully");

    // Close modal
    closeModalBtn?.addEventListener("click", () => {
        friendsModal.style.display = "none";
    });

    // Close modal when clicking outside
    window.addEventListener("click", (e) => {
        if (e.target === friendsModal) {
            friendsModal.style.display = "none";
        }
    });

    // Tab switching
    friendsTab?.addEventListener("click", () => {
        friendsTab.classList.add("active");
        pendingTab.classList.remove("active");
        addFriendTab.classList.remove("active");
        friendsView.style.display = "block";
        pendingView.style.display = "none";
        addFriendView.style.display = "none";
        loadFriendsList();
    });

    pendingTab?.addEventListener("click", () => {
        pendingTab.classList.add("active");
        friendsTab.classList.remove("active");
        addFriendTab.classList.remove("active");
        pendingView.style.display = "block";
        friendsView.style.display = "none";
        addFriendView.style.display = "none";
        loadPendingRequests();
    });

    addFriendTab?.addEventListener("click", () => {
        addFriendTab.classList.add("active");
        friendsTab.classList.remove("active");
        pendingTab.classList.remove("active");
        addFriendView.style.display = "block";
        friendsView.style.display = "none";
        pendingView.style.display = "none";
        friendSearchInput.value = "";
        searchResults.innerHTML = '<p class="search-empty">Enter a username to search</p>';
    });

    // Load friends list
    async function loadFriendsList() {
        console.log("Loading friends list for user:", currentUserId);
        friendsList.innerHTML = '<p class="friends-loading">Loading friends...</p>';
        
        try {
            const url = `friendship?action=list&userId=${currentUserId}`;
            console.log("Fetching:", url);
            
            const res = await fetch(url);
            console.log("Response status:", res.status);
            console.log("Response ok:", res.ok);
            
            const data = await res.json();
            console.log("Friends data:", data);
            
            if (!data.success) {
                console.error("Error from servlet:", data.message);
                friendsList.innerHTML = '<p class="friends-empty">Error loading friends</p>';
                return;
            }
            
            const friends = data.friends || [];
            console.log("Number of friends:", friends.length);
            
            if (friends.length === 0) {
                friendsList.innerHTML = '<p class="friends-empty">No friends yet</p>';
                return;
            }
            
            friendsList.innerHTML = "";
            
            for (const friend of friends) {
                console.log("Creating card for friend:", friend);
                const friendCard = await createFriendCard(friend);
                friendsList.appendChild(friendCard);
            }
            
            console.log("Friends list loaded successfully");
            
        } catch (err) {
            console.error("Error loading friends:", err);
            console.error("Error stack:", err.stack);
            friendsList.innerHTML = '<p class="friends-empty">Error loading friends</p>';
        }
    }

    // Load pending friend requests
    async function loadPendingRequests() {
        console.log("Loading pending requests for user:", currentUserId);
        pendingList.innerHTML = '<p class="friends-loading">Loading requests...</p>';
        
        try {
            const url = `friendship?action=pending&userId=${currentUserId}`;
            console.log("Fetching:", url);
            
            const res = await fetch(url);
            const data = await res.json();
            
            console.log("Pending requests data:", data);
            
            if (!data.success) {
                console.error("Error from servlet:", data.message);
                pendingList.innerHTML = '<p class="friends-empty">Error loading requests</p>';
                return;
            }
            
            const requests = data.requests || [];
            console.log("Number of pending requests:", requests.length);
            
            if (requests.length === 0) {
                pendingList.innerHTML = '<p class="friends-empty">No pending requests</p>';
                return;
            }
            
            pendingList.innerHTML = "";
            
            for (const request of requests) {
                console.log("Creating card for request:", request);
                const requestCard = await createPendingRequestCard(request);
                pendingList.appendChild(requestCard);
            }
            
            console.log("Pending requests loaded successfully");
            
        } catch (err) {
            console.error("Error loading pending requests:", err);
            console.error("Error stack:", err.stack);
            pendingList.innerHTML = '<p class="friends-empty">Error loading requests</p>';
        }
    }

    // Create pending request card
    async function createPendingRequestCard(request) {
        const card = document.createElement("div");
        card.className = "friend-card";
        
        // Fetch requester's profile details
        let profileData = null;
        try {
            const res = await fetch(`profile?userId=${request.userId}`);
            const data = await res.json();
            if (data.success) {
                profileData = data.profile;
            }
        } catch (err) {
            console.error("Error fetching requester profile:", err);
        }
        
        const displayName = request.displayName || request.username;
        const isVisible = profileData?.profileVisibility || false;
        const capital = isVisible && profileData?.availableCapital ? 
            `${profileData.availableCapital.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}` : 
            "Hidden";
        
        card.innerHTML = `
            <div class="friend-info">
                <img src="default_profile.jpg" alt="${displayName}" class="friend-avatar">
                <div class="friend-details">
                    <div class="friend-name">${displayName}</div>
                    <div class="friend-capital">Capital: ${capital}</div>
                </div>
            </div>
            <div class="request-actions">
                <button class="btn btn-primary btn-sm accept-btn" data-friendship-id="${request.friendshipId}">
                    Accept
                </button>
                <button class="btn btn-danger btn-sm decline-btn" data-friendship-id="${request.friendshipId}">
                    Decline
                </button>
            </div>
        `;
        
        // Accept button handler
        const acceptBtn = card.querySelector(".accept-btn");
        acceptBtn.addEventListener("click", async () => {
            await acceptFriendRequest(request.friendshipId);
        });
        
        // Decline button handler
        const declineBtn = card.querySelector(".decline-btn");
        declineBtn.addEventListener("click", async () => {
            await declineFriendRequest(request.friendshipId);
        });
        
        return card;
    }

    // Accept friend request
    async function acceptFriendRequest(friendshipId) {
        try {
            const res = await fetch("friendship", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "accept",
                    userId: currentUserId,
                    friendshipId: friendshipId
                })
            });
            
            const data = await res.json();
            
            if (data.success) {
                alert("Friend request accepted!");
                loadPendingRequests();
                // Also refresh friends list in case user switches tabs
            } else {
                alert("Failed to accept request: " + (data.message || "Unknown error"));
            }
        } catch (err) {
            console.error("Error accepting friend request:", err);
            alert("Error accepting friend request");
        }
    }

    // Decline friend request
    async function declineFriendRequest(friendshipId) {
        try {
            const res = await fetch("friendship", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "decline",
                    userId: currentUserId,
                    friendshipId: friendshipId
                })
            });
            
            const data = await res.json();
            
            if (data.success) {
                loadPendingRequests();
            } else {
                alert("Failed to decline request: " + (data.message || "Unknown error"));
            }
        } catch (err) {
            console.error("Error declining friend request:", err);
            alert("Error declining friend request");
        }
    }

    // Create friend card
    async function createFriendCard(friend) {
        const card = document.createElement("div");
        card.className = "friend-card";
        
        // Fetch friend's profile details
        let profileData = null;
        try {
            const res = await fetch(`profile?userId=${friend.userId}`);
            const data = await res.json();
            if (data.success) {
                profileData = data.profile;
            }
        } catch (err) {
            console.error("Error fetching friend profile:", err);
        }
        
        const displayName = friend.displayName || friend.username;
        const isVisible = profileData?.profileVisibility || false;
        const capital = isVisible && profileData?.availableCapital ? 
            `${profileData.availableCapital.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}` : 
            "Hidden";
        
        card.innerHTML = `
            <div class="friend-info">
                <img src="default_profile.jpg" alt="${displayName}" class="friend-avatar">
                <div class="friend-details">
                    <div class="friend-name">${displayName}</div>
                    <div class="friend-capital">Capital: ${capital}</div>
                </div>
            </div>
            <button class="btn btn-danger btn-sm remove-friend-btn" data-friend-id="${friend.userId}">
                Remove
            </button>
        `;
        
        // Remove friend handler
        const removeBtn = card.querySelector(".remove-friend-btn");
        removeBtn.addEventListener("click", async () => {
            if (confirm(`Remove ${displayName} from friends?`)) {
                await removeFriend(friend.userId);
            }
        });
        
        return card;
    }

    // Remove friend
    async function removeFriend(friendId) {
        try {
            const res = await fetch("friendship", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "remove",
                    userId: currentUserId,
                    friendId: friendId
                })
            });
            
            const data = await res.json();
            
            if (data.success) {
                loadFriendsList();
            } else {
                alert("Failed to remove friend: " + (data.message || "Unknown error"));
            }
        } catch (err) {
            console.error("Error removing friend:", err);
            alert("Error removing friend");
        }
    }

    // Search for users
    friendSearchInput?.addEventListener("input", (e) => {
        const query = e.target.value.trim();
        
        clearTimeout(searchTimeout);
        
        if (query.length === 0) {
            searchResults.innerHTML = '<p class="search-empty">Enter a username to search</p>';
            return;
        }
        
        searchResults.innerHTML = '<p class="friends-loading">Searching...</p>';
        
        searchTimeout = setTimeout(() => searchUsers(query), 300);
    });

    async function searchUsers(username) {
        try {
            // Search by username - we'll need to query all users
            const res = await fetch(`leaderboard?page=1&pageSize=100`);
            const data = await res.json();
            
            if (!data.success) {
                searchResults.innerHTML = '<p class="search-empty">Error searching users</p>';
                return;
            }
            
            const users = data.rankings || [];
            const matchedUser = users.find(u => 
                u.username.toLowerCase() === username.toLowerCase()
            );
            
            if (!matchedUser) {
                searchResults.innerHTML = '<p class="search-empty">No user found</p>';
                return;
            }
            
            if (matchedUser.userId === currentUserId) {
                searchResults.innerHTML = '<p class="search-empty">Cannot add yourself</p>';
                return;
            }
            
            // Check if already friends
            const friendsRes = await fetch(`friendship?action=list&userId=${currentUserId}`);
            const friendsData = await friendsRes.json();
            const friends = friendsData.friends || [];
            const alreadyFriend = friends.some(f => f.userId === matchedUser.userId);
            
            if (alreadyFriend) {
                searchResults.innerHTML = '<p class="search-empty">Already friends</p>';
                return;
            }
            
            // Fetch user profile
            let profileData = null;
            try {
                const profileRes = await fetch(`profile?userId=${matchedUser.userId}`);
                const profileDataRes = await profileRes.json();
                if (profileDataRes.success) {
                    profileData = profileDataRes.profile;
                }
            } catch (err) {
                console.error("Error fetching profile:", err);
            }
            
            const displayName = matchedUser.displayName || matchedUser.username;
            const isVisible = profileData?.profileVisibility || false;
            const capital = isVisible && profileData?.availableCapital ? 
                `${profileData.availableCapital.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}` : 
                "Hidden";
            
            searchResults.innerHTML = `
                <div class="friend-card">
                    <div class="friend-info">
                        <img src="default_profile.jpg" alt="${displayName}" class="friend-avatar">
                        <div class="friend-details">
                            <div class="friend-name">${displayName}</div>
                            <div class="friend-capital">Capital: ${capital}</div>
                        </div>
                    </div>
                    <button class="btn btn-primary btn-sm add-friend-btn" data-user-id="${matchedUser.userId}">
                        Add Friend
                    </button>
                </div>
            `;
            
            // Add friend handler
            const addBtn = searchResults.querySelector(".add-friend-btn");
            addBtn.addEventListener("click", async () => {
                await sendFriendRequest(matchedUser.userId);
            });
            
        } catch (err) {
            console.error("Error searching users:", err);
            searchResults.innerHTML = '<p class="search-empty">Error searching users</p>';
        }
    }

    // Send friend request
    async function sendFriendRequest(friendId) {
        try {
            const res = await fetch("friendship", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    action: "send",
                    userId: currentUserId,
                    friendId: friendId
                })
            });
            
            const data = await res.json();
            
            if (data.success) {
                alert("Friend request sent!");
                friendSearchInput.value = "";
                searchResults.innerHTML = '<p class="search-empty">Friend request sent successfully!</p>';
            } else {
                alert("Failed to send friend request: " + (data.message || "Unknown error"));
            }
        } catch (err) {
            console.error("Error sending friend request:", err);
            alert("Error sending friend request");
        }
    }
}

/* Save button click */
document.getElementById("saveProfileBtn")?.addEventListener("click", saveProfile);

/* ---------------------------------------------------------
   Friends List Functionality
--------------------------------------------------------- */