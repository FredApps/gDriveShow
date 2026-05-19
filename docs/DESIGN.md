# Design Direction

## Principles

- TV first: every primary action must work with D-pad, select, and back.
- Browse calmly: large targets, stable layout, clear focus rings, no dense phone-style lists.
- Keep media central: thumbnails and preview dominate; metadata stays secondary.
- Avoid setup friction during daily use: authorization and settings should stay out of the main path.

## First Screen

The app opens directly to the Drive browser, not a landing page. The layout uses three zones:

- Left navigation: Drive, Slideshows, Settings.
- Center grid: folder, image, and video tiles.
- Right preview panel: selected item preview, metadata, and primary action.

## Visual Language

- Dark neutral background for TV contrast.
- Warm accent colors per content tile so placeholders are legible before thumbnails load.
- 8 dp or smaller corner radius for app surfaces.
- Strong white or accent focus border on active tiles.
- Large headings only for page-level context; compact panels use tighter text.

## Browse States

- Loading: skeleton tile grid and preview placeholder.
- Empty folder: centered empty state with action to go up one folder.
- Error: concise message, retry button, and sign-in reset path when auth fails.
- Offline cache: visible stale-state indicator when data is cached.

## Slideshow Controls

- Select: pause or resume.
- Left/right: previous or next item.
- Back: return to folder.
- Long select: open lightweight playback options.

## Settings Shape

- Google Drive account.
- Starting folder.
- Slideshow interval.
- Image fit: contain, cover, or fill.
- Video behavior: play once, loop current, or continue to next.
- Cache size.

