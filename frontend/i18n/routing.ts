import { defineRouting } from "next-intl/routing";

// Matches the backend's locale support: LocaleConfig defaults to English
// and switches via a "ne" param (messages.properties / messages_ne.properties).
export const routing = defineRouting({
  locales: ["en", "ne"],
  defaultLocale: "en",
});
