'use client';

import { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import Header from '@/components/Header';
import { categoryApi, providerApi, bookingApi, Category, ProviderMatch } from '@/lib/api';
import { useBookingWizardStore, useAuthStore } from '@/lib/store';
import { getCategoryIcon, formatCurrency, getTimeSlotLabel, getLanguageLabel } from '@/lib/utils';
import { ArrowLeft, ArrowRight, MapPin, Clock, Star, Shield, Check, Loader2, Search } from 'lucide-react';

function BookingWizard() {
    const router = useRouter();
    const searchParams = useSearchParams();
    const { isAuthenticated } = useAuthStore();

    const {
        step, setStep,
        categoryId, categoryName, setCategory,
        postalCode, city, canton, setLocation,
        description, setDescription,
        preferredDate, preferredTimeSlot, setPreferredTime,
        selectedProviderId, selectedProviderName, setSelectedProvider,
        budgetMin, budgetMax, setBudget,
        reset,
    } = useBookingWizardStore();

    const [categories, setCategories] = useState<Category[]>([]);
    const [providers, setProviders] = useState<ProviderMatch[]>([]);
    const [loading, setLoading] = useState(false);
    const [searchLoading, setSearchLoading] = useState(false);
    const [error, setError] = useState('');

    // Load categories
    useEffect(() => {
        async function fetchCategories() {
            try {
                const data = await categoryApi.getAll();
                setCategories(data);

                // Check if category is in URL
                const catSlug = searchParams.get('category');
                if (catSlug) {
                    const cat = data.find((c) => c.slug === catSlug);
                    if (cat) {
                        setCategory(cat.id, cat.name);
                        setStep(1);
                    }
                }
            } catch (error) {
                console.error('Failed to fetch categories:', error);
            }
        }
        fetchCategories();
    }, [searchParams, setCategory, setStep]);

    // Search providers when step 2 is reached
    const searchProviders = async () => {
        if (!categoryId || !postalCode || !city) return;

        setSearchLoading(true);
        setError('');

        try {
            const results = await providerApi.match({
                categoryId,
                postalCode,
                city,
                preferredTime: preferredDate && preferredTimeSlot
                    ? new Date(preferredDate).toISOString()
                    : undefined,
            });
            setProviders(results);
        } catch (err: any) {
            setError('Erreur lors de la recherche de prestataires');
        } finally {
            setSearchLoading(false);
        }
    };

    useEffect(() => {
        if (step === 2 && categoryId && postalCode && city) {
            searchProviders();
        }
    }, [step]);

    const handleSubmit = async () => {
        if (!isAuthenticated) {
            router.push(`/login?redirect=/book`);
            return;
        }

        setLoading(true);
        setError('');

        try {
            const booking = await bookingApi.create({
                categoryId: categoryId!,
                providerId: selectedProviderId || undefined,
                description,
                postalCode,
                city,
                canton: canton || 'JU',
                preferredDate: preferredDate || undefined,
                preferredTimeSlot: preferredTimeSlot || undefined,
                budgetMin: budgetMin || undefined,
                budgetMax: budgetMax || undefined,
            });
            reset();
            router.push(`/dashboard/bookings/${booking.id}?success=true`);
        } catch (err: any) {
            setError(err.response?.data?.message || 'Erreur lors de la création de la réservation');
            setLoading(false);
        }
    };

    const canProceed = () => {
        switch (step) {
            case 0: return !!categoryId;
            case 1: return !!postalCode && !!city && !!description;
            case 2: return true; // Provider is optional
            case 3: return true;
            default: return false;
        }
    };

    const steps = [
        { title: 'Service', subtitle: 'Choisissez votre service' },
        { title: 'Détails', subtitle: 'Décrivez votre besoin' },
        { title: 'Prestataire', subtitle: 'Sélectionnez un prestataire' },
        { title: 'Confirmation', subtitle: 'Vérifiez et confirmez' },
    ];

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="pt-20 pb-12">
                <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Progress Steps */}
                    <div className="mb-8">
                        <div className="flex items-center justify-between">
                            {steps.map((s, i) => (
                                <div key={i} className="flex-1 flex items-center">
                                    <div className="flex flex-col items-center relative">
                                        <div
                                            className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-semibold transition-colors ${i < step
                                                    ? 'bg-green-500 text-white'
                                                    : i === step
                                                        ? 'bg-violet-600 text-white'
                                                        : 'bg-gray-200 text-gray-500'
                                                }`}
                                        >
                                            {i < step ? <Check className="w-5 h-5" /> : i + 1}
                                        </div>
                                        <div className="hidden sm:block mt-2 text-center">
                                            <p className={`text-sm font-medium ${i <= step ? 'text-gray-900' : 'text-gray-400'}`}>
                                                {s.title}
                                            </p>
                                        </div>
                                    </div>
                                    {i < steps.length - 1 && (
                                        <div
                                            className={`flex-1 h-1 mx-2 ${i < step ? 'bg-green-500' : 'bg-gray-200'
                                                }`}
                                        />
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Step Content */}
                    <div className="bg-white rounded-2xl shadow-lg p-6 md:p-8">
                        {/* Step 0: Category Selection */}
                        {step === 0 && (
                            <div>
                                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                                    Quel service recherchez-vous ?
                                </h2>
                                <p className="text-gray-500 mb-6">
                                    Sélectionnez la catégorie qui correspond à votre besoin
                                </p>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                    {categories.map((cat) => (
                                        <button
                                            key={cat.id}
                                            onClick={() => setCategory(cat.id, cat.name)}
                                            className={`p-4 rounded-xl border-2 text-left transition-all ${categoryId === cat.id
                                                    ? 'border-violet-500 bg-violet-50'
                                                    : 'border-gray-200 hover:border-violet-300 hover:bg-gray-50'
                                                }`}
                                        >
                                            <div className="text-3xl mb-2">{getCategoryIcon(cat.icon)}</div>
                                            <h3 className="font-semibold text-gray-900">{cat.name}</h3>
                                            <p className="text-sm text-gray-500 mt-1">{cat.description}</p>
                                        </button>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Step 1: Details */}
                        {step === 1 && (
                            <div>
                                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                                    Décrivez votre besoin
                                </h2>
                                <p className="text-gray-500 mb-6">
                                    Plus vous êtes précis, mieux les prestataires pourront vous aider
                                </p>

                                <div className="space-y-6">
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                Code postal *
                                            </label>
                                            <input
                                                type="text"
                                                value={postalCode}
                                                onChange={(e) => setLocation(e.target.value, city, canton)}
                                                placeholder="2800"
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                Ville *
                                            </label>
                                            <input
                                                type="text"
                                                value={city}
                                                onChange={(e) => setLocation(postalCode, e.target.value, canton)}
                                                placeholder="Delémont"
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Description de votre besoin *
                                        </label>
                                        <textarea
                                            value={description}
                                            onChange={(e) => setDescription(e.target.value)}
                                            placeholder="Décrivez votre besoin en détail..."
                                            rows={4}
                                            className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 resize-none"
                                        />
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                Date souhaitée
                                            </label>
                                            <input
                                                type="date"
                                                value={preferredDate || ''}
                                                onChange={(e) => setPreferredTime(e.target.value, preferredTimeSlot)}
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                Créneau horaire
                                            </label>
                                            <select
                                                value={preferredTimeSlot || ''}
                                                onChange={(e) => setPreferredTime(preferredDate, e.target.value || null)}
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            >
                                                <option value="">Indifférent</option>
                                                <option value="MORNING">{getTimeSlotLabel('MORNING')}</option>
                                                <option value="AFTERNOON">{getTimeSlotLabel('AFTERNOON')}</option>
                                                <option value="EVENING">{getTimeSlotLabel('EVENING')}</option>
                                            </select>
                                        </div>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Budget (CHF)
                                        </label>
                                        <div className="grid grid-cols-2 gap-4">
                                            <input
                                                type="number"
                                                value={budgetMin || ''}
                                                onChange={(e) => setBudget(e.target.value ? Number(e.target.value) : null, budgetMax)}
                                                placeholder="Minimum"
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                            <input
                                                type="number"
                                                value={budgetMax || ''}
                                                onChange={(e) => setBudget(budgetMin, e.target.value ? Number(e.target.value) : null)}
                                                placeholder="Maximum"
                                                className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Step 2: Provider Selection */}
                        {step === 2 && (
                            <div>
                                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                                    Choisissez un prestataire
                                </h2>
                                <p className="text-gray-500 mb-6">
                                    Sélectionnez un prestataire ou laissez-nous trouver le meilleur pour vous
                                </p>

                                {searchLoading ? (
                                    <div className="flex flex-col items-center justify-center py-12">
                                        <Loader2 className="w-8 h-8 text-violet-600 animate-spin mb-4" />
                                        <p className="text-gray-500">Recherche des prestataires disponibles...</p>
                                    </div>
                                ) : providers.length > 0 ? (
                                    <div className="space-y-4">
                                        {/* Skip option */}
                                        <button
                                            onClick={() => setSelectedProvider(null, null)}
                                            className={`w-full p-4 rounded-xl border-2 text-left transition-all ${selectedProviderId === null
                                                    ? 'border-violet-500 bg-violet-50'
                                                    : 'border-dashed border-gray-300 hover:border-gray-400'
                                                }`}
                                        >
                                            <div className="flex items-center gap-4">
                                                <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center">
                                                    <Search className="w-6 h-6 text-gray-400" />
                                                </div>
                                                <div>
                                                    <h3 className="font-semibold text-gray-900">Laisser le choix</h3>
                                                    <p className="text-sm text-gray-500">
                                                        Nous trouverons le meilleur prestataire disponible pour vous
                                                    </p>
                                                </div>
                                            </div>
                                        </button>

                                        {/* Provider list */}
                                        {providers.map((provider) => (
                                            <button
                                                key={provider.id}
                                                onClick={() => setSelectedProvider(provider.id, provider.name)}
                                                className={`w-full p-4 rounded-xl border-2 text-left transition-all ${selectedProviderId === provider.id
                                                        ? 'border-violet-500 bg-violet-50'
                                                        : 'border-gray-200 hover:border-violet-300'
                                                    }`}
                                            >
                                                <div className="flex items-start gap-4">
                                                    <div className="w-14 h-14 rounded-full bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white text-xl font-semibold shrink-0">
                                                        {provider.name.charAt(0)}
                                                    </div>
                                                    <div className="flex-1 min-w-0">
                                                        <div className="flex items-center gap-2 flex-wrap">
                                                            <h3 className="font-semibold text-gray-900">{provider.name}</h3>
                                                            {provider.isVerified && (
                                                                <span className="verified-badge">
                                                                    <Shield className="w-3 h-3" />
                                                                    Vérifié
                                                                </span>
                                                            )}
                                                        </div>
                                                        <p className="text-sm text-gray-500 mt-1 line-clamp-2">{provider.bio}</p>
                                                        <div className="flex items-center gap-4 mt-2 text-sm text-gray-500">
                                                            {provider.averageRating && (
                                                                <span className="flex items-center gap-1">
                                                                    <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                                                                    {provider.averageRating.toFixed(1)} ({provider.ratingCount})
                                                                </span>
                                                            )}
                                                            <span className="flex items-center gap-1">
                                                                <MapPin className="w-4 h-4" />
                                                                {provider.city}
                                                            </span>
                                                            {provider.languages?.length > 0 && (
                                                                <span>{provider.languages.map(getLanguageLabel).join(', ')}</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                    <div className="text-right shrink-0">
                                                        {provider.hourlyRate && (
                                                            <p className="font-semibold text-violet-600">
                                                                {formatCurrency(provider.hourlyRate)}/h
                                                            </p>
                                                        )}
                                                        {provider.fixedPrice && (
                                                            <p className="font-semibold text-violet-600">
                                                                {formatCurrency(provider.fixedPrice)} forfait
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="text-center py-12">
                                        <p className="text-gray-500 mb-4">Aucun prestataire trouvé dans votre zone.</p>
                                        <p className="text-sm text-gray-400">
                                            Votre demande sera envoyée à tous les prestataires disponibles.
                                        </p>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Step 3: Confirmation */}
                        {step === 3 && (
                            <div>
                                <h2 className="text-2xl font-bold text-gray-900 mb-2">
                                    Récapitulatif de votre demande
                                </h2>
                                <p className="text-gray-500 mb-6">
                                    Vérifiez les informations avant de confirmer
                                </p>

                                {error && (
                                    <div className="mb-6 p-4 bg-red-50 border border-red-100 rounded-xl text-red-600 text-sm">
                                        {error}
                                    </div>
                                )}

                                <div className="space-y-4">
                                    <div className="p-4 bg-gray-50 rounded-xl">
                                        <p className="text-sm text-gray-500 mb-1">Service</p>
                                        <p className="font-semibold">{categoryName}</p>
                                    </div>

                                    <div className="p-4 bg-gray-50 rounded-xl">
                                        <p className="text-sm text-gray-500 mb-1">Localisation</p>
                                        <p className="font-semibold">{postalCode} {city}</p>
                                    </div>

                                    <div className="p-4 bg-gray-50 rounded-xl">
                                        <p className="text-sm text-gray-500 mb-1">Description</p>
                                        <p className="text-gray-700">{description}</p>
                                    </div>

                                    {preferredDate && (
                                        <div className="p-4 bg-gray-50 rounded-xl">
                                            <p className="text-sm text-gray-500 mb-1">Date souhaitée</p>
                                            <p className="font-semibold">
                                                {new Date(preferredDate).toLocaleDateString('fr-CH')}
                                                {preferredTimeSlot && ` - ${getTimeSlotLabel(preferredTimeSlot)}`}
                                            </p>
                                        </div>
                                    )}

                                    {selectedProviderName && (
                                        <div className="p-4 bg-violet-50 rounded-xl border border-violet-100">
                                            <p className="text-sm text-violet-600 mb-1">Prestataire sélectionné</p>
                                            <p className="font-semibold text-violet-700">{selectedProviderName}</p>
                                        </div>
                                    )}

                                    {(budgetMin || budgetMax) && (
                                        <div className="p-4 bg-gray-50 rounded-xl">
                                            <p className="text-sm text-gray-500 mb-1">Budget</p>
                                            <p className="font-semibold">
                                                {budgetMin && formatCurrency(budgetMin)}
                                                {budgetMin && budgetMax && ' - '}
                                                {budgetMax && formatCurrency(budgetMax)}
                                            </p>
                                        </div>
                                    )}
                                </div>

                                {!isAuthenticated && (
                                    <div className="mt-6 p-4 bg-yellow-50 border border-yellow-100 rounded-xl">
                                        <p className="text-yellow-700 text-sm">
                                            Vous devez vous connecter pour confirmer votre réservation.
                                        </p>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* Navigation */}
                        <div className="flex items-center justify-between mt-8 pt-6 border-t">
                            <button
                                onClick={() => step > 0 ? setStep(step - 1) : router.push('/')}
                                className="flex items-center gap-2 text-gray-600 hover:text-gray-900 font-medium transition-colors"
                            >
                                <ArrowLeft className="w-5 h-5" />
                                Retour
                            </button>

                            {step < 3 ? (
                                <button
                                    onClick={() => setStep(step + 1)}
                                    disabled={!canProceed()}
                                    className="flex items-center gap-2 px-6 py-3 rounded-xl btn-primary font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                                >
                                    Continuer
                                    <ArrowRight className="w-5 h-5" />
                                </button>
                            ) : (
                                <button
                                    onClick={handleSubmit}
                                    disabled={loading}
                                    className="flex items-center gap-2 px-6 py-3 rounded-xl btn-primary font-semibold disabled:opacity-50"
                                >
                                    {loading ? (
                                        <>
                                            <Loader2 className="w-5 h-5 animate-spin" />
                                            Envoi...
                                        </>
                                    ) : (
                                        <>
                                            Confirmer la demande
                                            <Check className="w-5 h-5" />
                                        </>
                                    )}
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default function BookPage() {
    return (
        <Suspense fallback={<div className="min-h-screen flex items-center justify-center"><div className="spinner"></div></div>}>
            <BookingWizard />
        </Suspense>
    );
}
