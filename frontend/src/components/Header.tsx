'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useAuthStore } from '@/lib/store';
import { Menu, X, User, LogOut, Settings, ChevronDown } from 'lucide-react';
import * as DropdownMenu from '@radix-ui/react-dropdown-menu';

export default function Header() {
    const { user, isAuthenticated, logout } = useAuthStore();
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

    return (
        <header className="fixed top-0 left-0 right-0 z-50 glass-card">
            <nav className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex items-center justify-between h-16">
                    {/* Logo */}
                    <Link href="/" className="flex items-center gap-2">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white font-bold text-lg">
                            S@
                        </div>
                        <span className="text-xl font-bold gradient-text hidden sm:block">
                            Serv@nitin
                        </span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="hidden md:flex items-center gap-8">
                        <Link
                            href="/categories"
                            className="text-gray-600 hover:text-violet-600 font-medium transition-colors"
                        >
                            Services
                        </Link>
                        <Link
                            href="/how-it-works"
                            className="text-gray-600 hover:text-violet-600 font-medium transition-colors"
                        >
                            Comment ça marche
                        </Link>
                        <Link
                            href="/become-provider"
                            className="text-gray-600 hover:text-violet-600 font-medium transition-colors"
                        >
                            Devenir prestataire
                        </Link>
                    </div>

                    {/* Auth Buttons */}
                    <div className="hidden md:flex items-center gap-4">
                        {isAuthenticated && user ? (
                            <DropdownMenu.Root>
                                <DropdownMenu.Trigger asChild>
                                    <button className="flex items-center gap-2 px-4 py-2 rounded-full bg-violet-50 hover:bg-violet-100 transition-colors">
                                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white text-sm font-medium">
                                            {user.name.charAt(0).toUpperCase()}
                                        </div>
                                        <span className="font-medium text-gray-700">{user.name.split(' ')[0]}</span>
                                        <ChevronDown className="w-4 h-4 text-gray-500" />
                                    </button>
                                </DropdownMenu.Trigger>
                                <DropdownMenu.Portal>
                                    <DropdownMenu.Content
                                        className="min-w-[200px] bg-white rounded-xl shadow-xl border border-gray-100 p-2 z-50"
                                        sideOffset={8}
                                        align="end"
                                    >
                                        <DropdownMenu.Item asChild>
                                            <Link
                                                href="/dashboard"
                                                className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-violet-50 transition-colors cursor-pointer"
                                            >
                                                <User className="w-4 h-4 text-violet-500" />
                                                <span>Mon espace</span>
                                            </Link>
                                        </DropdownMenu.Item>
                                        {user.role === 'PROVIDER' && (
                                            <DropdownMenu.Item asChild>
                                                <Link
                                                    href="/provider"
                                                    className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-violet-50 transition-colors cursor-pointer"
                                                >
                                                    <Settings className="w-4 h-4 text-violet-500" />
                                                    <span>Espace prestataire</span>
                                                </Link>
                                            </DropdownMenu.Item>
                                        )}
                                        {user.role === 'ADMIN' && (
                                            <DropdownMenu.Item asChild>
                                                <Link
                                                    href="/admin"
                                                    className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-violet-50 transition-colors cursor-pointer"
                                                >
                                                    <Settings className="w-4 h-4 text-violet-500" />
                                                    <span>Administration</span>
                                                </Link>
                                            </DropdownMenu.Item>
                                        )}
                                        <DropdownMenu.Separator className="my-2 h-px bg-gray-100" />
                                        <DropdownMenu.Item
                                            className="flex items-center gap-3 px-4 py-3 rounded-lg hover:bg-red-50 text-red-600 transition-colors cursor-pointer"
                                            onClick={logout}
                                        >
                                            <LogOut className="w-4 h-4" />
                                            <span>Déconnexion</span>
                                        </DropdownMenu.Item>
                                    </DropdownMenu.Content>
                                </DropdownMenu.Portal>
                            </DropdownMenu.Root>
                        ) : (
                            <>
                                <Link
                                    href="/login"
                                    className="px-4 py-2 text-violet-600 font-medium hover:text-violet-700 transition-colors"
                                >
                                    Connexion
                                </Link>
                                <Link
                                    href="/register"
                                    className="px-6 py-2 rounded-full btn-primary font-medium"
                                >
                                    Inscription
                                </Link>
                            </>
                        )}
                    </div>

                    {/* Mobile menu button */}
                    <button
                        className="md:hidden p-2 rounded-lg hover:bg-gray-100 transition-colors"
                        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                    >
                        {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
                    </button>
                </div>

                {/* Mobile menu */}
                {mobileMenuOpen && (
                    <div className="md:hidden py-4 border-t border-gray-100">
                        <div className="flex flex-col gap-2">
                            <Link
                                href="/categories"
                                className="px-4 py-3 rounded-lg hover:bg-violet-50 font-medium transition-colors"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                Services
                            </Link>
                            <Link
                                href="/how-it-works"
                                className="px-4 py-3 rounded-lg hover:bg-violet-50 font-medium transition-colors"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                Comment ça marche
                            </Link>
                            <Link
                                href="/become-provider"
                                className="px-4 py-3 rounded-lg hover:bg-violet-50 font-medium transition-colors"
                                onClick={() => setMobileMenuOpen(false)}
                            >
                                Devenir prestataire
                            </Link>
                            <hr className="my-2" />
                            {isAuthenticated ? (
                                <>
                                    <Link
                                        href="/dashboard"
                                        className="px-4 py-3 rounded-lg hover:bg-violet-50 font-medium transition-colors"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Mon espace
                                    </Link>
                                    <button
                                        className="px-4 py-3 rounded-lg hover:bg-red-50 text-red-600 font-medium text-left transition-colors"
                                        onClick={() => {
                                            logout();
                                            setMobileMenuOpen(false);
                                        }}
                                    >
                                        Déconnexion
                                    </button>
                                </>
                            ) : (
                                <>
                                    <Link
                                        href="/login"
                                        className="px-4 py-3 rounded-lg hover:bg-violet-50 font-medium transition-colors"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Connexion
                                    </Link>
                                    <Link
                                        href="/register"
                                        className="mx-4 py-3 rounded-full btn-primary font-medium text-center"
                                        onClick={() => setMobileMenuOpen(false)}
                                    >
                                        Inscription
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>
                )}
            </nav>
        </header>
    );
}
