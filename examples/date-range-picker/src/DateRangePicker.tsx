import { useEffect, useMemo, useRef, useState } from "react";
import type { RefObject } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { addDays, addMonths, clamp, diffDays, formatHeaderLabel, monthShort, startOfDay, startOfMonth } from "./dateUtils";

type Range = { startDay: number; endDay: number };
type DragMode = "move" | "resize-left" | "resize-right" | null;
type Preset = { id: string; label: string; getRange: () => Range };

const TODAY = startOfDay(new Date());
const TIMELINE_START = startOfMonth(addMonths(TODAY, -4));
const TOTAL_DAYS = diffDays(TIMELINE_START, TODAY);
const HANDLE_SIZE = 18;
const PILL_HEIGHT = 40;

const PRESETS: Preset[] = [
  {
    id: "month",
    label: "This month",
    getRange: () => ({ startDay: diffDays(TIMELINE_START, startOfMonth(TODAY)), endDay: TOTAL_DAYS }),
  },
  { id: "7d", label: "Last 7D", getRange: () => ({ startDay: TOTAL_DAYS - 6, endDay: TOTAL_DAYS }) },
  { id: "30d", label: "30D", getRange: () => ({ startDay: TOTAL_DAYS - 29, endDay: TOTAL_DAYS }) },
  { id: "90d", label: "90D", getRange: () => ({ startDay: Math.max(0, TOTAL_DAYS - 89), endDay: TOTAL_DAYS }) },
];

const SPRING = { type: "spring" as const, stiffness: 380, damping: 32, mass: 0.8 };
const INSTANT = { type: "tween" as const, duration: 0 };

function dayDate(index: number): Date {
  return addDays(TIMELINE_START, index);
}

function useElementWidth(ref: RefObject<HTMLElement | null>): number {
  const [width, setWidth] = useState(0);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new ResizeObserver((entries) => {
      const entry = entries[0];
      if (entry) setWidth(entry.contentRect.width);
    });
    setWidth(el.getBoundingClientRect().width);
    observer.observe(el);
    return () => observer.disconnect();
  }, [ref]);

  return width;
}

export default function DateRangePicker() {
  const trackRef = useRef<HTMLDivElement>(null);
  const trackWidth = useElementWidth(trackRef);
  const pxPerDay = trackWidth > 0 ? trackWidth / TOTAL_DAYS : 0;

  const [range, setRange] = useState<Range>(() => PRESETS[0].getRange());
  const [dragMode, setDragMode] = useState<DragMode>(null);
  const [isHovering, setIsHovering] = useState(false);

  const activePresetId = useMemo(() => {
    const match = PRESETS.find((preset) => {
      const presetRange = preset.getRange();
      return presetRange.startDay === range.startDay && presetRange.endDay === range.endDay;
    });
    return match?.id ?? null;
  }, [range]);

  const startDate = dayDate(range.startDay);
  const endDate = dayDate(range.endDay);
  const dayCount = range.endDay - range.startDay + 1;
  const headerLabel = formatHeaderLabel(startDate, endDate, TODAY);

  const xForDay = (day: number) => day * pxPerDay;
  const pillLeft = xForDay(range.startDay);
  const pillWidth = Math.max(xForDay(range.endDay) - xForDay(range.startDay), 0);
  const transition = dragMode ? INSTANT : SPRING;

  const monthTicks = useMemo(() => {
    const ticks: { day: number; label: string }[] = [];
    for (let d = new Date(TIMELINE_START); d <= TODAY; d = addDays(d, 1)) {
      if (d.getDate() === 1) {
        ticks.push({ day: diffDays(TIMELINE_START, d), label: monthShort(d) });
      }
    }
    return ticks;
  }, []);

  const minorTicks = useMemo(
    () => Array.from({ length: Math.floor(TOTAL_DAYS / 7) + 1 }, (_, i) => i * 7),
    [],
  );

  function applyPreset(preset: Preset) {
    setRange(preset.getRange());
  }

  function beginDrag(mode: Exclude<DragMode, null>) {
    return (event: React.PointerEvent) => {
      if (pxPerDay === 0) return;
      event.preventDefault();
      event.stopPropagation();

      const startClientX = event.clientX;
      const origin = range;
      const minGapDays = Math.max(1, Math.ceil(28 / pxPerDay));
      setDragMode(mode);

      const handleMove = (moveEvent: PointerEvent) => {
        const deltaDay = (moveEvent.clientX - startClientX) / pxPerDay;

        if (mode === "move") {
          const span = origin.endDay - origin.startDay;
          const newStart = clamp(Math.round(origin.startDay + deltaDay), 0, TOTAL_DAYS - span);
          setRange({ startDay: newStart, endDay: newStart + span });
        } else if (mode === "resize-left") {
          const newStart = clamp(Math.round(origin.startDay + deltaDay), 0, origin.endDay - minGapDays);
          setRange({ startDay: newStart, endDay: origin.endDay });
        } else {
          const newEnd = clamp(Math.round(origin.endDay + deltaDay), origin.startDay + minGapDays, TOTAL_DAYS);
          setRange({ startDay: origin.startDay, endDay: newEnd });
        }
      };

      const handleUp = () => {
        window.removeEventListener("pointermove", handleMove);
        window.removeEventListener("pointerup", handleUp);
        setDragMode(null);
      };

      window.addEventListener("pointermove", handleMove);
      window.addEventListener("pointerup", handleUp);
    };
  }

  const showBadge = (dragMode !== null || isHovering) && pxPerDay > 0;

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
      className="w-full max-w-[640px] rounded-[24px] bg-white p-8 shadow-[0_24px_70px_-20px_rgba(15,23,42,0.18)] ring-1 ring-slate-900/5"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-slate-400">Date range</p>
          <h2 className="mt-1.5 text-[22px] font-semibold tracking-tight text-slate-900">{headerLabel}</h2>
        </div>
        <SegmentedControl activeId={activePresetId} onSelect={applyPreset} />
      </div>

      <div className="relative mt-10 select-none" style={{ paddingLeft: HANDLE_SIZE / 2, paddingRight: HANDLE_SIZE / 2 }}>
        <div ref={trackRef} className="relative h-[92px]">
          <div className="absolute left-0 right-0 top-12 h-px bg-slate-200" />

          {minorTicks.map((day) => (
            <div
              key={`minor-${day}`}
              className="absolute top-12 h-1.5 w-px bg-slate-200"
              style={{ left: xForDay(day) }}
            />
          ))}

          {monthTicks.map((tick) => (
            <div key={`month-${tick.day}`} style={{ left: xForDay(tick.day) }} className="absolute top-12">
              <div className="h-2.5 w-px bg-slate-300" />
              <div className="absolute top-[14px] -translate-x-1/2 whitespace-nowrap text-[10px] font-medium uppercase tracking-wide text-slate-400">
                {tick.label}
              </div>
            </div>
          ))}

          <div
            className="absolute top-0 h-12 w-px border-l border-dashed border-slate-300"
            style={{ left: xForDay(TOTAL_DAYS) }}
          />

          <motion.div
            className="absolute top-0 cursor-grab touch-none active:cursor-grabbing"
            style={{ height: PILL_HEIGHT }}
            animate={{ left: pillLeft, width: pillWidth }}
            transition={transition}
            onPointerDown={beginDrag("move")}
            onPointerEnter={() => setIsHovering(true)}
            onPointerLeave={() => setIsHovering(false)}
          >
            <div className="absolute inset-0 rounded-full bg-gradient-to-r from-indigo-500 to-violet-500 shadow-[0_12px_28px_-8px_rgba(99,102,241,0.55)] ring-1 ring-inset ring-white/30" />
            <Handle side="left" active={dragMode === "resize-left"} onPointerDown={beginDrag("resize-left")} />
            <Handle side="right" active={dragMode === "resize-right"} onPointerDown={beginDrag("resize-right")} />
          </motion.div>

          <motion.div
            className="absolute -top-3 -translate-x-1/2"
            animate={{ left: pillLeft + pillWidth / 2 }}
            transition={transition}
          >
            <AnimatePresence>
              {showBadge && (
                <motion.span
                  initial={{ opacity: 0, y: 6, scale: 0.85 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, y: 6, scale: 0.85 }}
                  transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  className="block -translate-y-full whitespace-nowrap rounded-full bg-slate-900 px-2.5 py-1 text-[12px] font-semibold text-white shadow-lg"
                >
                  {dayCount} {dayCount === 1 ? "Day" : "Days"}
                </motion.span>
              )}
            </AnimatePresence>
          </motion.div>
        </div>
      </div>
    </motion.div>
  );
}

function Handle({
  side,
  active,
  onPointerDown,
}: {
  side: "left" | "right";
  active: boolean;
  onPointerDown: (event: React.PointerEvent) => void;
}) {
  return (
    <motion.div
      onPointerDown={onPointerDown}
      className={`absolute top-1/2 flex h-[18px] w-[18px] -translate-y-1/2 cursor-ew-resize touch-none items-center justify-center rounded-full bg-white shadow-md ring-1 ring-slate-200 ${
        side === "left" ? "-left-[9px]" : "-right-[9px]"
      }`}
      whileHover={{ scale: 1.15 }}
      whileTap={{ scale: 0.9 }}
      animate={{ scale: active ? 1.15 : 1 }}
      transition={{ type: "spring", stiffness: 500, damping: 30 }}
    >
      <span className="h-2 w-px bg-slate-300" />
      <span className="ml-[2px] h-2 w-px bg-slate-300" />
    </motion.div>
  );
}

function SegmentedControl({
  activeId,
  onSelect,
}: {
  activeId: string | null;
  onSelect: (preset: Preset) => void;
}) {
  return (
    <div className="inline-flex max-w-full items-center gap-1 overflow-x-auto rounded-full bg-slate-100 p-1">
      {PRESETS.map((preset) => {
        const active = preset.id === activeId;
        return (
          <button
            key={preset.id}
            type="button"
            onClick={() => onSelect(preset)}
            className="relative shrink-0 whitespace-nowrap rounded-full px-3.5 py-1.5 text-[13px] font-medium transition-colors duration-200"
          >
            {active && (
              <motion.span
                layoutId="segmented-active-pill"
                className="absolute inset-0 rounded-full bg-white shadow-[0_1px_4px_rgba(15,23,42,0.12)]"
                transition={{ type: "spring", stiffness: 500, damping: 36 }}
              />
            )}
            <span className={`relative ${active ? "text-slate-900" : "text-slate-500 hover:text-slate-700"}`}>
              {preset.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}
