import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { getMemberProfile } from "@/lib/api";

// No separate marketing landing page -- visiting the bare domain goes
// straight to sign-in (or the dashboard if already signed in), per the
// site owner's call: this is a private family record, not a public
// site that needs its own hero/pitch page. /about, /history,
// /membership, /contact, /privacy, /terms are unaffected -- only the
// bare "/" changes.
export default async function RootPage() {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.getAll().map((c) => `${c.name}=${c.value}`).join("; ");

  const profile = await getMemberProfile(cookieHeader);
  redirect(profile.kind === "unauthenticated" ? "/login" : "/dashboard");
}
