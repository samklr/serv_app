'use client';

import { useState, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { authApi } from '@/lib/api';
import { ArrowLeft, Loader2, Eye, EyeOff, CheckCircle, XCircle, Lock } from 'lucide-react';

function ResetPasswordContent() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const token = searchParams.get('token');

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState('');

    const passwordRequirements = [
        { label: 'Au moins 8 caractères', met: password.length >= 8 },
        { label: 'Une lettre majuscule', met: /[A-Z]/.test(password) },
        { label: 'Une lettre minuscule', met: /[a-z]/.test(password) },
        { label: 'Un chiffre', met: /[0-9]/.test(password) },
        { label: 'Un caractère spécial', met: /[!@#$%^&*(),.?":{}|<>]/.test(password) },
    ];

    const allRequirementsMet = passwordRequirements.every((req) => req.met);
    const passwordsMatch = password === confirmPassword && confirmPassword.length > 0;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');

        if (!token) {
            setError('Token de réinitialisation manquant.');
            return;
        }

        if (!allRequirementsMet) {
            setError('Le mot de passe ne respecte pas tous les critères.');
            return;
        }

        if (!passwordsMatch) {
            setError('Les mots de passe ne correspondent pas.');
            return;
        }

        setLoading(true);

        try {
            await authApi.confirmPasswordReset(token, password);
            setSuccess(true);
            setTimeout(() => {
                router.push('/login');
            }, 3000);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Le lien de réinitialisation est invalide ou a expiré.');
        } finally {
            setLoading(false);
        }
    };

    if (!token) {
        return (
            <div className="min-h-screen hero-gradient bg-pattern flex items-center justify-center p-4">
                <div className="w-full max-w-md">
                    <Link
                        href="/"
                        className="inline-flex items-center gap-2 text-gray-600 hover:text-violet-600 mb-8 transition-colors"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        Retour à l'accueil
                    </Link>

                    <div className="bg-white rounded-2xl shadow-xl p-8">
                        <div className="text-center">
                            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <XCircle className="w-10 h-10 text-red-600" />
                            </div>
                            <h1 className="text-2xl font-bold text-gray-900 mb-2">Lien invalide</h1>
                            <p className="text-gray-600 mb-6">
                                Ce lien de réinitialisation est invalide ou a expiré.
                            </p>
                            <Link
                                href="/forgot-password"
                                className="inline-block w-full py-3 rounded-xl btn-primary font-semibold text-center"
                            >
                                Demander un nouveau lien
                            </Link>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen hero-gradient bg-pattern flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                <Link
                    href="/login"
                    className="inline-flex items-center gap-2 text-gray-600 hover:text-violet-600 mb-8 transition-colors"
                >
                    <ArrowLeft className="w-4 h-4" />
                    Retour à la connexion
                </Link>

                <div className="bg-white rounded-2xl shadow-xl p-8">
                    <div className="text-center mb-8">
                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white font-bold text-2xl mx-auto mb-4">
                            S@
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900">Nouveau mot de passe</h1>
                        <p className="text-gray-500 mt-2">
                            Créez un nouveau mot de passe sécurisé
                        </p>
                    </div>

                    {success ? (
                        <div className="text-center py-4">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <CheckCircle className="w-10 h-10 text-green-600" />
                            </div>
                            <h2 className="text-xl font-semibold text-gray-900 mb-2">Mot de passe modifié !</h2>
                            <p className="text-gray-600 mb-4">
                                Votre mot de passe a été réinitialisé avec succès.
                            </p>
                            <p className="text-sm text-gray-500">
                                Redirection vers la connexion...
                            </p>
                        </div>
                    ) : (
                        <>
                            {error && (
                                <div className="mb-6 p-4 bg-red-50 border border-red-100 rounded-xl text-red-600 text-sm">
                                    {error}
                                </div>
                            )}

                            <form onSubmit={handleSubmit} className="space-y-6">
                                <div>
                                    <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                                        Nouveau mot de passe
                                    </label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                        <input
                                            id="password"
                                            type={showPassword ? 'text' : 'password'}
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="••••••••"
                                            required
                                            className="w-full pl-12 pr-12 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword(!showPassword)}
                                            className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                        >
                                            {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                </div>

                                {password && (
                                    <div className="p-4 bg-gray-50 rounded-xl space-y-2">
                                        <p className="text-sm font-medium text-gray-700 mb-2">Critères du mot de passe :</p>
                                        {passwordRequirements.map((req, index) => (
                                            <div key={index} className="flex items-center gap-2 text-sm">
                                                {req.met ? (
                                                    <CheckCircle className="w-4 h-4 text-green-600" />
                                                ) : (
                                                    <XCircle className="w-4 h-4 text-gray-300" />
                                                )}
                                                <span className={req.met ? 'text-green-700' : 'text-gray-500'}>
                                                    {req.label}
                                                </span>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                <div>
                                    <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
                                        Confirmer le mot de passe
                                    </label>
                                    <div className="relative">
                                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                        <input
                                            id="confirmPassword"
                                            type={showConfirmPassword ? 'text' : 'password'}
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            placeholder="••••••••"
                                            required
                                            className="w-full pl-12 pr-12 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                            className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                                        >
                                            {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>
                                    {confirmPassword && !passwordsMatch && (
                                        <p className="mt-2 text-sm text-red-600">Les mots de passe ne correspondent pas</p>
                                    )}
                                    {passwordsMatch && (
                                        <p className="mt-2 text-sm text-green-600 flex items-center gap-1">
                                            <CheckCircle className="w-4 h-4" />
                                            Les mots de passe correspondent
                                        </p>
                                    )}
                                </div>

                                <button
                                    type="submit"
                                    disabled={loading || !allRequirementsMet || !passwordsMatch}
                                    className="w-full py-3 rounded-xl btn-primary font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {loading ? (
                                        <>
                                            <Loader2 className="w-5 h-5 animate-spin" />
                                            Réinitialisation...
                                        </>
                                    ) : (
                                        'Réinitialiser le mot de passe'
                                    )}
                                </button>
                            </form>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}

export default function ResetPasswordPage() {
    return (
        <Suspense fallback={
            <div className="min-h-screen hero-gradient bg-pattern flex items-center justify-center p-4">
                <div className="text-center">
                    <Loader2 className="w-12 h-12 text-violet-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Chargement...</p>
                </div>
            </div>
        }>
            <ResetPasswordContent />
        </Suspense>
    );
}
