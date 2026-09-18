$f = 'D:/Code/猪小能/frontend/meowflow-ui/dist/assets/element-plus-F8lVQFm3.js'
$bytes = (Get-Item $f).Length
Write-Host "Bytes: $bytes"
$content = Get-Content $f -Raw
Write-Host "Length: $($content.Length)"
# Find big unique strings to identify what's heavy
Write-Host '--- Components mentioned ---'
$names = 'Button','Dialog','Input','Table','Form','Select','Card','Tabs','Dropdown','Menu','Icon','Tooltip','Pagination','Drawer','Descriptions','Message','MessageBox','Popover','Loading','Switch','Tag','Row','Col','Option','Checkbox','Radio','TableColumn','DatePicker','Upload','Text','Link','Divider','Progress','Badge','Avatar','Image','Collapse','Affix','Backtop','Skeleton','Empty','Result','Cascader','Tree','TreeSelect','ColorPicker','Transfer','Slider','Rate','TimePicker','TimeSelect','Segmented','Watermark','Space','Steps','Tour','Anchor','PageHeader','Mentions','Submenu','Aside','Header','Main','Footer','Container','InputNumber','ButtonGroup','Popconfirm','Tooltip','Drawer','Dialog','Carousel','Collapse','Calendar','Cascader','Mention','Statistic','Timeline','Upload','Watermark','Anchor','Segmented','Slider','InfiniteScroll','Autocomplete','AutoComplete'
foreach ($n in $names) {
  if ($content -match [regex]::Escape("component_$($n.ToLower())") -or $content -match [regex]::Escape("$($n.ToLower())-")) {
    Write-Host "FOUND: $n"
  }
}
Write-Host '--- check imports ---'
# Count chunks from element-plus/es subpaths
$sub = [regex]::Matches($content, 'element-plus/es/components/[^"\\s]+').Count
Write-Host "element-plus subpath refs in chunk: $sub"
# Count direct exports from element-plus
$exp = [regex]::Matches($content, 'element-plus/es/components/[^"\\s]+/index\.mjs').Count
Write-Host "Subpath index.mjs refs: $exp"
