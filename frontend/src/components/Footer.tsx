import Link from 'next/link';

export default function Footer() {
    return (
        <footer className="bg-gray-900 text-white">
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
                    {/* Brand */}
                    <div className="md:col-span-1">
                        <Link href="/" className="flex items-center gap-2">
                            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white font-bold text-lg">
                                S@
                            </div>
                            <span className="text-xl font-bold">Serv@nitin</span>
                        </Link>
                        <p className="mt-4 text-gray-400 text-sm leading-relaxed">
                            La marketplace des services à domicile en Suisse romande.
                            Trouvez des prestataires de confiance près de chez vous.
                        </p>
                    </div>

                    {/* Services */}
                    <div>
                        <h3 className="font-semibold text-lg mb-4">Services</h3>
                        <ul className="space-y-2 text-gray-400">
                            <li>
                                <Link href="/categories/babysitting" className="hover:text-white transition-colors">
                                    Babysitting
                                </Link>
                            </li>
                            <li>
                                <Link href="/categories/home-support" className="hover:text-white transition-colors">
                                    Bricolage
                                </Link>
                            </li>
                            <li>
                                <Link href="/categories/elderly-support" className="hover:text-white transition-colors">
                                    Aide aux seniors
                                </Link>
                            </li>
                            <li>
                                <Link href="/categories/tax-admin" className="hover:text-white transition-colors">
                                    Aide administrative
                                </Link>
                            </li>
                        </ul>
                    </div>

                    {/* Company */}
                    <div>
                        <h3 className="font-semibold text-lg mb-4">Entreprise</h3>
                        <ul className="space-y-2 text-gray-400">
                            <li>
                                <Link href="/about" className="hover:text-white transition-colors">
                                    À propos
                                </Link>
                            </li>
                            <li>
                                <Link href="/become-provider" className="hover:text-white transition-colors">
                                    Devenir prestataire
                                </Link>
                            </li>
                            <li>
                                <Link href="/blog" className="hover:text-white transition-colors">
                                    Blog
                                </Link>
                            </li>
                            <li>
                                <Link href="/contact" className="hover:text-white transition-colors">
                                    Contact
                                </Link>
                            </li>
                        </ul>
                    </div>

                    {/* Legal */}
                    <div>
                        <h3 className="font-semibold text-lg mb-4">Légal</h3>
                        <ul className="space-y-2 text-gray-400">
                            <li>
                                <Link href="/terms" className="hover:text-white transition-colors">
                                    Conditions générales
                                </Link>
                            </li>
                            <li>
                                <Link href="/privacy" className="hover:text-white transition-colors">
                                    Politique de confidentialité
                                </Link>
                            </li>
                            <li>
                                <Link href="/cookies" className="hover:text-white transition-colors">
                                    Cookies
                                </Link>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className="mt-12 pt-8 border-t border-gray-800 flex flex-col md:flex-row justify-between items-center gap-4">
                    <p className="text-gray-500 text-sm">
                        © 2026 Serv@nitin. Tous droits réservés.
                    </p>
                    <div className="flex items-center gap-4">
                        <span className="text-gray-500 text-sm">Fait avec ❤️ en Suisse</span>
                    </div>
                </div>
            </div>
        </footer>
    );
}
