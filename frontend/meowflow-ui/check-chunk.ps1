$lines = Select-String -Path 'D:/Code/猪小能/frontend/meowflow-ui/dist/assets/element-plus-F8lVQFm3.js' -Pattern 'ElButton|ElDialog|ElInput|ElTable|ElButtonGroup|ElInputNumber|ElForm|ElSelect|ElCard|ElTabs|ElDropdown|ElMenu|ElIcon|ElTooltip|ElPagination|ElDrawer|ElDescriptions|ElMessage|ElMessageBox|ElPopover|ElLoading|ElSwitch|ElTag|ElRow|ElCol|ElOption|ElCheckbox|ElRadio|ElTableColumn|ElDatePicker|ElUpload|ElText|ElLink|ElDivider|ElProgress|ElBadge|ElAvatar|ElImage|ElCollapseItem|ElAffix|ElBacktop|ElSkeleton|ElEmpty|ElResult|ElCascader|ElTree|ElTreeSelect|ElColorPicker|ElTransfer|ElSlider|ElRate|ElTimePicker|ElTimeSelect|ElSegmented|ElWatermark|ElSpace|ElSteps|ElStep|ElTour|ElAnchor|ElPageHeader|ElMentions|ElSubmenu|ElAside|ElHeader|ElMain|ElFooter|ElContainer'
foreach ($l in $lines) {
  Write-Host $l.Line.Substring(0, [Math]::Min(180, $l.Line.Length))
}
Write-Host '---COUNT---'
Write-Host $lines.Count
