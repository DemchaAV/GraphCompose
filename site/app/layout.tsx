import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-inter",
  display: "swap",
});
const mono = JetBrains_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: "GraphCompose — Declarative Java DSL for business PDFs",
  description:
    "Declarative Java DSL for cinematic business PDFs. Two-pass deterministic layout, snapshot-tested in CI. MIT. Renders via Apache PDFBox 3.0.",
  metadataBase: new URL("https://graphcompose.dev"),
  openGraph: {
    title: "GraphCompose",
    description: "Declarative Java DSL for cinematic business PDFs.",
    type: "website",
  },
};

// Set the theme before first paint to avoid a flash of the wrong theme.
const themeInit = `(function(){try{var t=localStorage.getItem('gc-theme');if(t)document.documentElement.setAttribute('data-theme',t);}catch(e){}})();`;

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="en"
      data-theme="light"
      className={`${inter.variable} ${mono.variable}`}
      suppressHydrationWarning
    >
      <body>
        <script dangerouslySetInnerHTML={{ __html: themeInit }} />
        {children}
      </body>
    </html>
  );
}
