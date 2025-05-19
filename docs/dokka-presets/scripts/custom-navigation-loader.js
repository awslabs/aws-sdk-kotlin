// Custom navigation loader for AWS SDK for Kotlin documentation.

// Extracts the module name from a given URL href
function extractModuleName(href) {
    try{
        const url = new URL(href, window.location.origin);
        const pathname = url.pathname;
        const pathSegments = pathname.split('/').filter(Boolean);

        // For local hosting
        if (url.hostname === 'localhost') {
            return pathSegments.length >= 1 ? pathSegments[0] : null;
        }

        return pathSegments.length >= 4 ? pathSegments[3] : null;
    }
    catch (error) {
        return null;
    }
}

// Hides the sidebar and adjusts main content layout
function hideSidebar() {
    const sidebar = document.getElementById('leftColumn');
    const main = document.getElementById('main');

    if (sidebar) {
        sidebar.style.display = 'none';
    }

    if (main) {
        main.style.marginLeft = '0';
        main.style.width = '100%';
    }
}

function loadNavigation() {
    const moduleName = extractModuleName(window.location.href);

    // Hide sidebar for root index page
    if (moduleName === "index.html") {
        hideSidebar()
        return Promise.resolve('');
    }

    const navigationPath = moduleName
        ? `${pathToRoot}${moduleName}/navigation.html`
        : `${pathToRoot}navigation.html`;

    return fetch(navigationPath)
        .then(response => response.text())
        .catch(error => {
            // Use root navigation as a fallback
            return fetch(pathToRoot + "navigation.html")
                .then(response => response.text());
        });
}

navigationPageText = loadNavigation()

// =================================================================
// Everything below this is copied from Dokka's navigation-loader.js
// =================================================================
displayNavigationFromPage = () => {
    navigationPageText.then(data => {
        document.getElementById("sideMenu").innerHTML = data;
    }).then(() => {
        document.querySelectorAll(".overview > a").forEach(link => {
            link.setAttribute("href", pathToRoot + link.getAttribute("href"));
        })
    }).then(() => {
        document.querySelectorAll(".sideMenuPart").forEach(nav => {
            if (!nav.classList.contains("hidden"))
                nav.classList.add("hidden")
        })
    }).then(() => {
        revealNavigationForCurrentPage()
    }).then(() => {
        scrollNavigationToSelectedElement()
    })
    document.querySelectorAll('.footer a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelector(this.getAttribute('href')).scrollIntoView({
                behavior: 'smooth'
            });
        });
    });
}

revealNavigationForCurrentPage = () => {
    let pageId = document.getElementById("content").attributes["pageIds"].value.toString();
    let parts = document.querySelectorAll(".sideMenuPart");
    let found = 0;
    do {
        parts.forEach(part => {
            if (part.attributes['pageId'].value.indexOf(pageId) !== -1 && found === 0) {
                found = 1;
                if (part.classList.contains("hidden")) {
                    part.classList.remove("hidden");
                    part.setAttribute('data-active', "");
                }
                revealParents(part)
            }
        });
        pageId = pageId.substring(0, pageId.lastIndexOf("/"))
    } while (pageId.indexOf("/") !== -1 && found === 0)
};
revealParents = (part) => {
    if (part.classList.contains("sideMenuPart")) {
        if (part.classList.contains("hidden"))
            part.classList.remove("hidden");
        revealParents(part.parentNode)
    }
};

scrollNavigationToSelectedElement = () => {
    let selectedElement = document.querySelector('div.sideMenuPart[data-active]')
    if (selectedElement == null) { // nothing selected, probably just the main page opened
        return
    }

    let hasIcon = selectedElement.querySelectorAll(":scope > div.overview span.nav-icon").length > 0

    // for instance enums also have children and are expandable, but are not package/module elements
    let isPackageElement = selectedElement.children.length > 1 && !hasIcon
    if (isPackageElement) {
        // if package is selected or linked, it makes sense to align it to top
        // so that you can see all the members it contains
        selectedElement.scrollIntoView(true)
    } else {
        // if a member within a package is linked, it makes sense to center it since it,
        // this should make it easier to look at surrounding members
        selectedElement.scrollIntoView({
            behavior: 'auto',
            block: 'center',
            inline: 'center'
        })
    }
}

/*
    This is a work-around for safari being IE of our times.
    It doesn't fire a DOMContentLoaded, presumabely because eventListener is added after it wants to do it
*/
if (document.readyState == 'loading') {
    window.addEventListener('DOMContentLoaded', () => {
        displayNavigationFromPage()
    })
} else {
    displayNavigationFromPage()
}
