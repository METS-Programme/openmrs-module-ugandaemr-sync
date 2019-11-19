<%
    ui.decorateWith("appui", "standardEmrPage", [title: ui.message("ugandaemrsync.title")])
%>
<script type="text/javascript">
    var breadcrumbs = [
        { icon: "icon-home", link: '/' + OPENMRS_CONTEXT_PATH + '/index.htm' },
        { label: "${ ui.message("coreapps.app.systemAdministration.label")}", link: '/' + OPENMRS_CONTEXT_PATH + '/coreapps/systemadministration/systemAdministration.page'},
        { label: "UgandaEMR Sync"},
    ];
</script>

${ ui.includeFragment("ugandaemrsync", "home") }