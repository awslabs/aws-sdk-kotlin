/**
 * Check for elements with a `toc--button` class, which indicates the sidebar has finished loading.
 */
async function dispatchNavigationLoadedEvent() {
    while (!document.querySelectorAll('.toc--button').length > 0) {
        await new Promise(resolve => setTimeout(resolve, 100));
    }
    window.dispatchEvent(new Event('navigationLoaded'));
}

document.addEventListener('DOMContentLoaded', dispatchNavigationLoadedEvent);
if (document.readyState === "interactive" || document.readyState === "complete" ) { dispatchNavigationLoadedEvent() }

/**
 * Apply "skip to main content" buttons after each active left sidebar `toc--part`.
 * These are invisible and only accessible via keyboard
 * Fixes accessibility violation: "Provide a mechanism for skipping past repetitive content"
 */
function applySkipLinks() {
    document.querySelectorAll('#content').forEach(function(contentDiv) {
        contentDiv.setAttribute('role', 'main');
        contentDiv.setAttribute('tabindex', '-1');
    });

    function insertSkipLink(element) {
        if (element.querySelectorAll(".skip-to-content").length > 0) { return }

        const skipLink = document.createElement('div');
        // Create an anchor element with the href pointing to the main content
        const anchor = document.createElement('a');
        anchor.classList.add('skip-to-content');
        anchor.href = '#content';
        anchor.innerHTML = 'Skip to Main Content';
        anchor.setAttribute("tabindex", "0");
        skipLink.appendChild(anchor);
        if (element.children.length > 1) {
            element.insertBefore(skipLink, element.children[1]);
        } else {
            element.appendChild(skipLink);
        }
    }

    function handleChanges(mutationsList) {
        for (const mutation of mutationsList) {
            if (mutation.type === 'attributes' && mutation.target.classList.contains('toc--part') && !mutation.target.classList.contains('toc--part_hidden')) {
                insertSkipLink(mutation.target);
            }
        }
    }

    // Insert a skip link on all visible toc-parts
    document.querySelectorAll('.toc--part:not(.toc--part_hidden)').forEach(function(sideMenuPart) {
        insertSkipLink(sideMenuPart)
    });

    // Insert a skip link on the first toc-part, regardless of visibility.
    const firstSideMenuPart = document.getElementById("sideMenu").children[0].querySelectorAll(".toc--part")[0]
    insertSkipLink(firstSideMenuPart)

    const observer = new MutationObserver(handleChanges);
    const observerConfig = {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ['class']
    };
    observer.observe(document.body, observerConfig);
}
window.addEventListener('navigationLoaded', applySkipLinks);
