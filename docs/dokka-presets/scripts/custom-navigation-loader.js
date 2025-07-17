// Custom navigation loader for AWS SDK for Kotlin documentation.

/**
 * Extracts the module name from a given URL href. For example:
 * https://sdk.amazonaws.com/kotlin/api/latest/index.html -> index.html
 * https://sdk.amazonaws.com/kotlin/api/latest/s3/index.html -> s3
 * https://sdk.amazonaws.com/kotlin/api/latest/s3/aws.sdk.kotlin.services.s3/index.html -> s3
 * https://sdk.amazonaws.com/kotlin/api/1.4.109/s3/index.html -> s3
 */
function extractModulePath(href) {
    try {
        const segments = new URL(href, window.location.origin)
            .pathname
            .split('/')
            .filter(Boolean); // drop empty parts

        // the URL pattern is always ".../kotlin/api/<version>/..." in production
        const apiIndex = segments.indexOf('api');

        if (apiIndex !== -1) {
            // segment after "api" is the version ("latest", "1.4.109", etc.)
            // segment after _that_ is the module name (or "index.html" if we're at the root)
            return segments[apiIndex + 2] ?? 'index.html';
        }

        // locally-hosted docs don't have /kotlin/api segment
        return segments[0] ?? 'index.html';
    } catch {
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
    const modulePath = extractModulePath(window.location.href);

    // Hide sidebar for root index page
    if (modulePath === "index.html") {
        hideSidebar()
        return Promise.resolve('');
    }

    const navigationPath = modulePath ? `${pathToRoot}${modulePath}/navigation.html` : `${pathToRoot}navigation.html`;

    return fetch(navigationPath)
        .then(response => response.text())
        .catch(error => {
            // Use root navigation as a fallback
            return fetch(pathToRoot + "navigation.html")
                .then(response => response.text());
        });
}

navigationPageText = loadNavigation()

/**
 * This is copied from Dokka's navigation-loader.js
 * https://github.com/Kotlin/dokka/blob/65b51bc0bc0496141f741bb9a61efcb4f3cc4633/dokka-subprojects/plugin-base/src/main/resources/dokka/scripts/navigation-loader.js
 * with a slight edit made to use `navigationPageText` instead of reading from `navigation.html`
 */
const TOC_STATE_KEY_PREFIX = 'TOC_STATE::';
const TOC_CONTAINER_ID = 'sideMenu';
const TOC_SCROLL_CONTAINER_ID = 'leftColumn';
const TOC_PART_CLASS = 'toc--part';
const TOC_PART_HIDDEN_CLASS = 'toc--part_hidden';
const TOC_LINK_CLASS = 'toc--link';
const TOC_SKIP_LINK_CLASS = 'toc--skip-link';

(function () {
    function displayToc() {
        navigationPageText
            .then((tocHTML) => {
                renderToc(tocHTML);
                updateTocLinks();
                collapseTocParts();
                expandTocPathToCurrentPage();
                restoreTocExpandedState();
                restoreTocScrollTop();
            });
    }

    function renderToc(tocHTML) {
        const containerElement = document.getElementById(TOC_CONTAINER_ID);
        if (containerElement) {
            containerElement.innerHTML = tocHTML;
        }
    }

    function updateTocLinks() {
        document.querySelectorAll(`.${TOC_LINK_CLASS}`).forEach((tocLink) => {
            tocLink.setAttribute('href', `${pathToRoot}${tocLink.getAttribute('href')}`);
            tocLink.addEventListener('keydown', preventScrollBySpaceKey);
        });
        document.querySelectorAll(`.${TOC_SKIP_LINK_CLASS}`).forEach((skipLink) => {
            skipLink.setAttribute('href', `#main`);
            skipLink.addEventListener('keydown', preventScrollBySpaceKey);
        })
    }

    function collapseTocParts() {
        document.querySelectorAll(`.${TOC_PART_CLASS}`).forEach((tocPart) => {
            if (!tocPart.classList.contains(TOC_PART_HIDDEN_CLASS)) {
                tocPart.classList.add(TOC_PART_HIDDEN_CLASS);
                const tocToggleButton = tocPart.querySelector('button');
                if (tocToggleButton) {
                    tocToggleButton.setAttribute("aria-expanded", "false");
                }
            }
        });
    }

    const expandTocPathToCurrentPage = () => {
        const tocParts = [...document.querySelectorAll(`.${TOC_PART_CLASS}`)];
        const currentPageId = document.getElementById('content')?.getAttribute('pageIds');
        if (!currentPageId) {
            return;
        }

        let isPartFound = false;
        let currentPageIdPrefix = currentPageId;
        while (!isPartFound && currentPageIdPrefix !== '') {
            tocParts.forEach((part) => {
                const partId = part.getAttribute('pageId');
                if (!isPartFound && partId?.includes(currentPageIdPrefix)) {
                    isPartFound = true;
                    expandTocPart(part);
                    expandTocPathToParent(part);
                    part.dataset.active = 'true';
                }
            });
            currentPageIdPrefix = currentPageIdPrefix.substring(0, currentPageIdPrefix.lastIndexOf('/'));
        }
    };

    const expandTocPathToParent = (part) => {
        if (part.classList.contains(TOC_PART_CLASS)) {
            expandTocPart(part);
            expandTocPathToParent(part.parentNode);
        }
    };

    const expandTocPart = (tocPart) => {
        if (tocPart.classList.contains(TOC_PART_HIDDEN_CLASS)) {
            tocPart.classList.remove(TOC_PART_HIDDEN_CLASS);
            const tocToggleButton = tocPart.querySelector('button');
            if (tocToggleButton) {
                tocToggleButton.setAttribute("aria-expanded", "true");
            }
            const tocPartId = tocPart.getAttribute('id');
            safeSessionStorage.setItem(`${TOC_STATE_KEY_PREFIX}${tocPartId}`, 'true');
        }
    };

    /**
     * Restores the state of the navigation tree from the local storage.
     * LocalStorage keys are in the format of `TOC_STATE::${id}` where `id` is the id of the part
     */
    const restoreTocExpandedState = () => {
        const allLocalStorageKeys = safeSessionStorage.getKeys();
        const tocStateKeys = allLocalStorageKeys.filter((key) => key.startsWith(TOC_STATE_KEY_PREFIX));
        tocStateKeys.forEach((key) => {
            const isExpandedTOCPart = safeSessionStorage.getItem(key) === 'true';
            const tocPartId = key.substring(TOC_STATE_KEY_PREFIX.length);
            const tocPart = document.querySelector(`.toc--part[id="${tocPartId}"]`);
            if (tocPart !== null && isExpandedTOCPart) {
                tocPart.classList.remove(TOC_PART_HIDDEN_CLASS);
                const tocToggleButton = tocPart.querySelector('button');
                if (tocToggleButton) {
                    tocToggleButton.setAttribute("aria-expanded", "true");
                }
            }
        });
    };

    function saveTocScrollTop() {
        const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
        if (container) {
            const currentScrollTop = container.scrollTop;
            safeSessionStorage.setItem(`${TOC_STATE_KEY_PREFIX}SCROLL_TOP`, `${currentScrollTop}`);
        }
    }

    function restoreTocScrollTop() {
        const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
        if (container) {
            const storedScrollTop = safeSessionStorage.getItem(`${TOC_STATE_KEY_PREFIX}SCROLL_TOP`);
            if (storedScrollTop) {
                container.scrollTop = Number(storedScrollTop);
            }
        }
    }

    function initTocScrollListener() {
        const container = document.getElementById(TOC_SCROLL_CONTAINER_ID);
        if (container) {
            container.addEventListener('scroll', saveTocScrollTop);
        }
    }

    function preventScrollBySpaceKey(event) {
        if (event.key === ' ') {
            event.preventDefault();
            event.stopPropagation();
        }
    }

    function resetTocState() {
        const tocKeys = safeSessionStorage.getKeys();
        tocKeys.forEach((key) => {
            if (key.startsWith(TOC_STATE_KEY_PREFIX)) {
                safeSessionStorage.removeItem(key);
            }
        });
    }

    function initLogoClickListener() {
        const logo = document.querySelector('.library-name--link');
        if (logo) {
            logo.addEventListener('click', resetTocState);
        }
    }

    /*
      This is a work-around for safari being IE of our times.
      It doesn't fire a DOMContentLoaded, presumably because eventListener is added after it wants to do it
  */
    if (document.readyState === 'loading') {
        window.addEventListener('DOMContentLoaded', () => {
            displayToc();
            initTocScrollListener();
            initLogoClickListener();
        })
    } else {
        displayToc();
        initTocScrollListener();
        initLogoClickListener();
    }
})();


function handleTocButtonClick(event, navId) {
    const tocPart = document.getElementById(navId);
    if (!tocPart) {
        return;
    }
    tocPart.classList.toggle(TOC_PART_HIDDEN_CLASS);
    const isExpandedTOCPart = !tocPart.classList.contains(TOC_PART_HIDDEN_CLASS);
    const button = tocPart.querySelector('button');
    button?.setAttribute("aria-expanded", `${isExpandedTOCPart}`);
    safeSessionStorage.setItem(`${TOC_STATE_KEY_PREFIX}${navId}`, `${isExpandedTOCPart}`);
}

/**
 * This is an unedited snippet of Dokka's safe-local-storage-blocking.js, pulling in `safeSessionStorage`
 * https://github.com/Kotlin/dokka/blob/83b0f8ad9ad920df0d842caa9c43d69e6e2c44f6/dokka-subprojects/plugin-base/src/main/resources/dokka/scripts/safe-local-storage_blocking.js
 */
/** When Dokka is viewed via iframe, session storage could be inaccessible (see https://github.com/Kotlin/dokka/issues/3323)
 * This is a wrapper around session storage to prevent errors in such cases
 * */
const safeSessionStorage = (() => {
    let isSessionStorageAvailable = false;
    try {
        const testKey = '__testSessionStorageKey__';
        sessionStorage.setItem(testKey, testKey);
        sessionStorage.removeItem(testKey);
        isSessionStorageAvailable = true;
    } catch (e) {
        console.error('Session storage is not available', e);
    }

    return {
        getItem: (key) => {
            if (!isSessionStorageAvailable) {
                return null;
            }
            return sessionStorage.getItem(key);
        },
        setItem: (key, value) => {
            if (!isSessionStorageAvailable) {
                return;
            }
            sessionStorage.setItem(key, value);
        },
        removeItem: (key) => {
            if (!isSessionStorageAvailable) {
                return;
            }
            sessionStorage.removeItem(key);
        },
        getKeys: () => {
            if (!isSessionStorageAvailable) {
                return [];
            }
            return Object.keys(sessionStorage);
        },
    };
})();