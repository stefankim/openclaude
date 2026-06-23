# Premium Date Range Picker

A minimalist, high-end date range picker built with React, TypeScript, Tailwind CSS v4, and Framer Motion.

## Features

- Dynamic header label (e.g. "March 1 – Today") that updates live as the range changes
- Segmented control (`This month` / `Last 7D` / `30D` / `90D`) with a sliding active-tab indicator
- Horizontal timeline scrubber with weekly tick marks, month labels, and a "today" marker
- Draggable range pill with independent left/right resize handles
- A floating "X Days" badge that appears on hover/drag and tracks the pill in real time
- Spring-based Framer Motion transitions for preset jumps and drag-release settling, with 1:1 instant tracking while actively dragging

## Run it

```bash
npm install
npm run dev
```

Then open the printed local URL. `npm run build` produces a static production build in `dist/`.

## Files

- `src/DateRangePicker.tsx` — the component
- `src/dateUtils.ts` — small date math/formatting helpers
- `src/App.tsx`, `src/main.tsx` — demo harness that renders the picker on its own
