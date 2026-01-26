'use client';

import { useState } from 'react';
import Link from 'next/link';
import { authApi } from '@/lib/api';
import { ArrowLeft, Loader2, Mail, CheckCircle } from 'lucide-react';

export default function ForgotPasswordPage() {
    const [email, setEmail] = useState('');
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            await authApi.requestPasswordReset(email);
            setSuccess(true);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Une erreur est survenue. Veuillez réessayer.');
        } finally {
            setLoading(false);
        }
    };

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
                        <h1 className="text-2xl font-bold text-gray-900">Mot de passe oublié</h1>
                        <p className="text-gray-500 mt-2">
                            Entrez votre email pour recevoir un lien de réinitialisation
                        </p>
                    </div>

                    {success ? (
                        <div className="text-center py-4">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <CheckCircle className="w-10 h-10 text-green-600" />
                            </div>
                            <h2 className="text-xl font-semibold text-gray-900 mb-2">Email envoyé !</h2>
                            <p className="text-gray-600 mb-6">
                                Si un compte existe avec l'adresse <strong>{email}</strong>, vous recevrez un email avec les instructions de réinitialisation.
                            </p>
                            <p className="text-sm text-gray-500 mb-4">
                                Vérifiez également votre dossier spam.
                            </p>
                            <Link
                                href="/login"
                                className="inline-block w-full py-3 rounded-xl btn-secondary font-semibold text-center"
                            >
                                Retour à la connexion
                            </Link>
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
                                    <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                                        Adresse email
                                    </label>
                                    <div className="relative">
                                        <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                                        <input
                                            id="email"
                                            type="email"
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            placeholder="vous@exemple.com"
                                            required
                                            className="w-full pl-12 pr-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                                        />
                                    </div>
                                </div>

                                <button
                                    type="submit"
                                    disabled={loading}
                                    className="w-full py-3 rounded-xl btn-primary font-semibold disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                                >
                                    {loading ? (
                                        <>
                                            <Loader2 className="w-5 h-5 animate-spin" />
                                            Envoi en cours...
                                        </>
                                    ) : (
                                        'Envoyer le lien de réinitialisation'
                                    )}
                                </button>
                            </form>

                            <div className="mt-6 text-center">
                                <p className="text-sm text-gray-500">
                                    Vous vous souvenez de votre mot de passe ?{' '}
                                    <Link href="/login" className="text-violet-600 hover:text-violet-700 font-medium">
                                        Se connecter
                                    </Link>
                                </p>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    );
}
