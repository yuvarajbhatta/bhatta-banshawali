import { getTranslations } from "next-intl/server";
import { getBsDateForAd } from "@/lib/api";

interface BirthDateDisplayProps {
  dateAd: string;
}

// dateAd is a date-only ISO string ("YYYY-MM-DD"); parsed by splitting
// rather than `new Date(dateAd)` to avoid UTC-midnight timezone shift.
export async function BirthDateDisplay({ dateAd }: BirthDateDisplayProps) {
  const t = await getTranslations("common");
  const adMonths = t.raw("adMonths") as string[];
  const bsMonths = t.raw("bsMonths") as string[];
  const bs = await getBsDateForAd(dateAd);

  const [yearStr, monthStr, dayStr] = dateAd.split("-");
  const adMonthName = adMonths[Number(monthStr) - 1] ?? monthStr;
  const adFormatted = `${adMonthName} ${Number(dayStr)}, ${yearStr}`;

  if (!bs) {
    return <>{adFormatted}</>;
  }

  const bsMonthName = bsMonths[bs.month - 1] ?? String(bs.month);
  const bsFormatted = `${bsMonthName} ${bs.day}, ${bs.year}`;

  return (
    <>
      {adFormatted} {t("adLabel")} ({bsFormatted} {t("bsLabel")})
    </>
  );
}
