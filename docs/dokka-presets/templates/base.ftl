<#-- This is an unchanged copy of Dokka's base.ftl -->
<#import "includes/page_metadata.ftl" as page_metadata>
<#import "includes/header.ftl" as header>
<#import "includes/footer.ftl" as footer>
<!DOCTYPE html>
<html class="no-js" lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1" charset="UTF-8">
    <@page_metadata.display/>
    <#-- AWS Analytics tracking script - Implements site-wide analytics and tracking functionality -->
    <script src="https://a0.awsstatic.com/s_code/js/3.0/awshome_s_code.js"></script>
    <@template_cmd name="pathToRoot"><script>var pathToRoot = "${pathToRoot}";</script></@template_cmd>
    <script>document.documentElement.classList.replace("no-js","js");</script>
    <#-- This script doesn't need to be there but it is nice to have
    since app in dark mode doesn't 'blink' (class is added before it is rendered) -->
    <script>const storage = localStorage.getItem("dokka-dark-mode")
        if (storage == null) {
            const osDarkSchemePreferred = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            if (osDarkSchemePreferred === true) {
                document.getElementsByTagName("html")[0].classList.add("theme-dark")
            }
        } else {
            const savedDarkMode = JSON.parse(storage)
            if(savedDarkMode === true) {
                document.getElementsByTagName("html")[0].classList.add("theme-dark")
            }
        }
    </script>
    <#-- Scripts for onboarding AWS Shortbread - Manages cookie consent banners and user preferences to help ensure
    compliance with privacy regulations like GDPR -->
    <script>var existingShortbreadEl = document.getElementById("awsccc-sb-ux-c");
    existingShortbreadEl && existingShortbreadEl.remove();
    </script>
    <script src="https://prod.assets.shortbread.aws.dev/shortbread.js"></script>
    <link href="https://prod.assets.shortbread.aws.dev/shortbread.css" rel="stylesheet">
    <script>const shortbread = AWSCShortbread();
    shortbread.checkForCookieConsent();
    </script>
    <#-- Resources (scripts, stylesheets) are handled by Dokka.
    Use customStyleSheets and customAssets to change them. -->
    <@resources/>
</head>
<body>
<div class="root">
    <@header.display/>
    <div id="container">
        <div class="sidebar" id="leftColumn">
            <div><a class="skip-to-content" href="#content" tabindex="0">Skip to Main Content</a></div>
            <div class="sidebar--inner" id="sideMenu"></div>
        </div>
        <div id="main">
            <@content/>
            <@footer.display/>
        </div>
    </div>
</div>
</body>
</html>
