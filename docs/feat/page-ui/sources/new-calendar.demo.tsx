"use client";

import { useState } from "react";
import { Calendar } from "@/components/ui/calendar";
import { format } from "date-fns";

export function CalendarPreview() {
  const [selected, setSelected] = useState<Date | undefined>(new Date(2026, 4, 6));

  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="rounded-2xl border border-border/50 bg-card p-6 shadow-sm">
        <Calendar
          defaultMonth={new Date(2026, 4)}
          onSelect={(date: Date | undefined) => setSelected(date)}
          selected={selected}
          size="sm"
          mode="single"
        />
        {selected && (
          <div className="mt-4 border-t border-border/50 pt-4">
            <p className="text-xs font-medium tracking-widest text-muted-foreground uppercase">
              Selected
            </p>
            <p className="text-base font-medium text-foreground mt-1">
              {format(selected, "EEEE, MMMM do, yyyy")}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

export default CalendarPreview;