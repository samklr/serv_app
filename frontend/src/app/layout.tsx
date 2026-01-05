import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: "Serv@nitin - Services à domicile en Suisse romande",
  description: "Trouvez rapidement des prestataires de confiance pour tous vos besoins : babysitting, bricolage, aide administrative, et plus encore.",
  keywords: "services, babysitting, bricolage, aide à domicile, Suisse, Jura, Delémont",
  openGraph: {
    title: "Serv@nitin - Services à domicile",
    description: "Marketplace de services à domicile en Suisse romande",
    type: "website",
    locale: "fr_CH",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <body className={`${inter.variable} font-sans antialiased`}>
        {children}
      </body>
    </html>
  );
}
