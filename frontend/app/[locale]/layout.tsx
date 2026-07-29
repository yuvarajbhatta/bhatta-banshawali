import type { Metadata } from "next";
import type { ReactNode } from "react";
import { hasLocale } from "next-intl";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { notFound } from "next/navigation";
import { Cormorant_Garamond, Inter } from "next/font/google";
import { routing } from "@/i18n/routing";
import "../globals.css";

// Exposed as CSS variables (see --font-sans/--font-serif in globals.css)
// rather than applied directly to <body>, so every existing CSS-module
// component that already reads those tokens picks the webfont up for
// free. Falls back to the system stack already baked into those tokens
// if a font fails to load -- not a hard dependency.
const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });
const cormorantGaramond = Cormorant_Garamond({
  subsets: ["latin"],
  weight: ["500", "600", "700"],
  variable: "--font-heritage-serif",
  display: "swap",
});

export const metadata: Metadata = {
  title: "Bhatta Banshawali",
  description: "A living record of the Bhatta family's history, preserved and verified.",
};

export function generateStaticParams() {
  return routing.locales.map((locale) => ({ locale }));
}

export default async function LocaleLayout({
  children,
  params,
}: {
  children: ReactNode;
  params: Promise<{ locale: string }>;
}) {
  const { locale } = await params;
  if (!hasLocale(routing.locales, locale)) {
    notFound();
  }

  const messages = await getMessages();

  return (
    <html lang={locale} className={`${inter.variable} ${cormorantGaramond.variable}`}>
      <body>
        <NextIntlClientProvider locale={locale} messages={messages}>
          {children}
        </NextIntlClientProvider>
      </body>
    </html>
  );
}
