'use client';

import { useEffect, useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { authApi } from '@/lib/api';
import { CheckCircle, XCircle, Loader2, ArrowLeft, Mail } from 'lucide-react';

function VerifyEmailContent() {
    const searchParams = useSearchParams();
    const token = searchParams.get('token');

    const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'no-token'>('loading');
    const [message, setMessage] = useState('');
    const [resendEmail, setResendEmail] = useState('');
    const [resendLoading, setResendLoading] = useState(false);
    const [resendSuccess, setResendSuccess] = useState(false);

    useEffect(() => {
        if (!token) {
            setStatus('no-token');
            return;
        }

        const verifyEmail = async () => {
            try {
                await authApi.verifyEmail(token);
                setStatus('success');
                setMessage('Votre email a été vérifié avec succès !');
            } catch (err: any) {
                setStatus('error');
                setMessage(err.response?.data?.message || 'Le lien de vérification est invalide ou a expiré.');
            }
        };

        verifyEmail();
    }, [token]);

    const handleResend = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!resendEmail) return;

        setResendLoading(true);
        try {
            await authApi.resendVerification(resendEmail);
            setResendSuccess(true);
        } catch (err: any) {
            setMessage(err.response?.data?.message || 'Impossible d\'envoyer l\'email de vérification.');
        } finally {
            setResendLoading(false);
        }
    };

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
                    <div className="text-center mb-8">
                        <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white font-bold text-2xl mx-auto mb-4">
                            S@
                        </div>
                        <h1 className="text-2xl font-bold text-gray-900">Vérification de l'email</h1>
                    </div>

                    {status === 'loading' && (
                        <div className="text-center py-8">
                            <Loader2 className="w-12 h-12 text-violet-600 animate-spin mx-auto mb-4" />
                            <p className="text-gray-600">Vérification en cours...</p>
                        </div>
                    )}

                    {status === 'success' && (
                        <div className="text-center py-8">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <CheckCircle className="w-10 h-10 text-green-600" />
                            </div>
                            <h2 className="text-xl font-semibold text-gray-900 mb-2">Email vérifié !</h2>
                            <p className="text-gray-600 mb-6">{message}</p>
                            <Link
                                href="/login"
                                className="inline-block w-full py-3 rounded-xl btn-primary font-semibold text-center"
                            >
                                Se connecter
                            </Link>
                        </div>
                    )}

                    {status === 'error' && (
                        <div className="text-center py-8">
                            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <XCircle className="w-10 h-10 text-red-600" />
                            </div>
                            <h2 className="text-xl font-semibold text-gray-900 mb-2">Échec de la vérification</h2>
                            <p className="text-gray-600 mb-6">{message}</p>

                            {!resendSuccess ? (
                                <form onSubmit={handleResend} className="space-y-4">
                                    <p className="text-sm text-gray-500">
                                        Entrez votre email pour recevoir un nouveau lien de vérification :
                                    </p>
                                    <input
                                        type="email"
                                        value={resendEmail}
                                        onChange={(e) => setResendEmail(e.target.value)}
                                        placeholder="vous@exemple.com"
                                        required
                                        className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                                    />
                                    <button
                                        type="submit"
                                        disabled={resendLoading}
                                        className="w-full py-3 rounded-xl btn-primary font-semibold disabled:opacity-50 flex items-center justify-center gap-2"
                                    >
                                        {resendLoading ? (
                                            <>
                                                <Loader2 className="w-5 h-5 animate-spin" />
                                                Envoi...
                                            </>
                                        ) : (
                                            <>
                                                <Mail className="w-5 h-5" />
                                                Renvoyer l'email
                                            </>
                                        )}
                                    </button>
                                </form>
                            ) : (
                                <div className="p-4 bg-green-50 rounded-xl text-green-700">
                                    Un nouvel email de vérification a été envoyé !
                                </div>
                            )}
                        </div>
                    )}

                    {status === 'no-token' && (
                        <div className="text-center py-8">
                            <div className="w-16 h-16 bg-yellow-100 rounded-full flex items-center justify-center mx-auto mb-4">
                                <Mail className="w-10 h-10 text-yellow-600" />
                            </div>
                            <h2 className="text-xl font-semibold text-gray-900 mb-2">Token manquant</h2>
                            <p className="text-gray-600 mb-6">
                                Aucun token de vérification trouvé. Vérifiez le lien dans votre email.
                            </p>

                            {!resendSuccess ? (
                                <form onSubmit={handleResend} className="space-y-4">
                                    <p className="text-sm text-gray-500">
                                        Entrez votre email pour recevoir un nouveau lien :
                                    </p>
                                    <input
                                        type="email"
                                        value={resendEmail}
                                        onChange={(e) => setResendEmail(e.target.value)}
                                        placeholder="vous@exemple.com"
                                        required
                                        className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                                    />
                                    <button
                                        type="submit"
                                        disabled={resendLoading}
                                        className="w-full py-3 rounded-xl btn-primary font-semibold disabled:opacity-50 flex items-center justify-center gap-2"
                                    >
                                        {resendLoading ? (
                                            <>
                                                <Loader2 className="w-5 h-5 animate-spin" />
                                                Envoi...
                                            </>
                                        ) : (
                                            <>
                                                <Mail className="w-5 h-5" />
                                                Envoyer l'email de vérification
                                            </>
                                        )}
                                    </button>
                                </form>
                            ) : (
                                <div className="p-4 bg-green-50 rounded-xl text-green-700">
                                    Un email de vérification a été envoyé !
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default function VerifyEmailPage() {
    return (
        <Suspense fallback={
            <div className="min-h-screen hero-gradient bg-pattern flex items-center justify-center p-4">
                <div className="text-center">
                    <Loader2 className="w-12 h-12 text-violet-600 animate-spin mx-auto mb-4" />
                    <p className="text-gray-600">Chargement...</p>
                </div>
            </div>
        }>
            <VerifyEmailContent />
        </Suspense>
    );
}
