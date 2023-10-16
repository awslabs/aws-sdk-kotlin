<#import "source_set_selector.ftl" as source_set_selector>
<#macro display>
<div class="navigation" id="navigation">
    <button id="menu-toggle" class="menu-toggle" aria-label="Open navigation menu"></button>
    <div class="library-name">
        <@template_cmd name="pathToRoot">
            <a href="${pathToRoot}index.html">
                <@template_cmd name="projectName">
                    <span>${projectName}</span>
                </@template_cmd>
            </a>
        </@template_cmd>
    </div>
    <div class="library-version">
        <#-- This can be handled by the versioning plugin -->
        <@version/>
    </div>
    <div class="pull-right d-flex">
        <@source_set_selector.display/>
        <button id="theme-toggle-button" class="navigation-controls--theme"><span id="theme-toggle"></span></button>
        <div id="searchBar"></div>
    </div>
</div>
</#macro>
