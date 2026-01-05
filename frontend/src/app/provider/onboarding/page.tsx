'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { ChevronRight, ChevronLeft, User, Briefcase, FileText, MapPin, Check, Upload, Plus, X } from 'lucide-react';
import { useAuthStore } from '@/lib/store';
import { categoryApi, providerApi, Category } from '@/lib/api';
import Header from '@/components/Header';
import Footer from '@/components/Footer';

const STEPS = [
    { id: 0, title: 'Profil', icon: User },
    { id: 1, title: 'Services', icon: Briefcase },
    { id: 2, title: 'Localisation', icon: MapPin },
    { id: 3, title: 'Vérification', icon: FileText },
];

const LANGUAGES = [
    { code: 'fr', name: 'Français' },
    { code: 'de', name: 'Allemand' },
    { code: 'it', name: 'Italien' },
    { code: 'en', name: 'Anglais' },
];

export default function ProviderOnboarding() {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [step, setStep] = useState(0);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [newCert, setNewCert] = useState('');

    const [data, setData] = useState({
        bio: '', categories: [] as string[], experience: '', certifications: [] as string[],
        languages: ['fr'], postalCode: '', city: '', serviceRadius: 20, hourlyRate: 30,
    });

    useEffect(() => {
        if (!isAuthenticated) { router.push('/login'); return; }
        if (user?.role !== 'PROVIDER') { router.push('/dashboard'); return; }
        categoryApi.getAll().then(setCategories).catch(console.error);
    }, [isAuthenticated, user, router]);

    const handleSubmit = async () => {
        setLoading(true); setError('');
        try {
            await providerApi.createProfile({
                bio: data.bio, categoryIds: data.categories.map(id => parseInt(id)), languages: data.languages,
                postalCode: data.postalCode, city: data.city, serviceRadius: data.serviceRadius,
                hourlyRate: data.hourlyRate, certifications: data.certifications,
                yearsOfExperience: parseInt(data.experience) || 0,
            });
            router.push('/provider/dashboard');
        } catch (err: unknown) {
            const error = err as { response?: { data?: { message?: string } } };
            setError(error.response?.data?.message || 'Une erreur est survenue');
        } finally { setLoading(false); }
    };

    const isValid = () => {
        if (step === 0) return data.bio.length >= 50;
        if (step === 1) return data.categories.length > 0 && data.languages.length > 0;
        if (step === 2) return data.postalCode && data.city && data.hourlyRate > 0;
        return true;
    };

    return (
        <><Header />
            <main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-purple-50 pt-24">
                <div className="container mx-auto px-4 py-8 max-w-3xl">
                    <h1 className="text-3xl font-bold text-gray-900 mb-2">Créez votre profil</h1>
                    <p className="text-gray-600 mb-8">Complétez votre profil pour recevoir des demandes</p>

                    {/* Progress */}
                    <div className="flex items-center justify-between mb-8">
                        {STEPS.map((s, i) => (
                            <div key={s.id} className="flex items-center">
                                <div className={`w-12 h-12 rounded-full flex items-center justify-center ${i < step ? 'bg-green-500 text-white' : i === step ? 'bg-purple-600 text-white' : 'bg-gray-200'}`}>
                                    {i < step ? <Check className="w-6 h-6" /> : <s.icon className="w-6 h-6" />}
                                </div>
                                {i < STEPS.length - 1 && <div className={`w-16 h-1 mx-2 ${i < step ? 'bg-green-500' : 'bg-gray-200'}`} />}
                            </div>
                        ))}
                    </div>

                    <div className="glass-effect rounded-2xl p-8">
                        {error && <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">{error}</div>}

                        {step === 0 && (
                            <div className="space-y-6">
                                <h2 className="text-xl font-semibold">Parlez-nous de vous</h2>
                                <div><label className="block text-sm font-medium mb-2">Bio *</label>
                                    <textarea value={data.bio} onChange={e => setData({ ...data, bio: e.target.value })} placeholder="Décrivez votre expérience..." className="w-full px-4 py-3 border rounded-lg" rows={5} />
                                    <p className="text-sm text-gray-500 mt-1">{data.bio.length}/50 min</p>
                                </div>
                                <div><label className="block text-sm font-medium mb-2">Années d&apos;expérience</label>
                                    <input type="number" value={data.experience} onChange={e => setData({ ...data, experience: e.target.value })} className="w-full px-4 py-3 border rounded-lg" />
                                </div>
                            </div>
                        )}

                        {step === 1 && (
                            <div className="space-y-6">
                                <h2 className="text-xl font-semibold">Vos services</h2>
                                <div><label className="block text-sm font-medium mb-3">Catégories *</label>
                                    <div className="grid grid-cols-2 gap-3">
                                        {categories.map(c => (
                                            <button key={c.id} onClick={() => setData({ ...data, categories: data.categories.includes(c.id) ? data.categories.filter(x => x !== c.id) : [...data.categories, c.id] })} className={`p-4 rounded-lg border-2 text-left ${data.categories.includes(c.id) ? 'border-purple-500 bg-purple-50' : 'border-gray-200'}`}>{c.name}</button>
                                        ))}
                                    </div>
                                </div>
                                <div><label className="block text-sm font-medium mb-3">Langues *</label>
                                    <div className="flex flex-wrap gap-2">
                                        {LANGUAGES.map(l => (
                                            <button key={l.code} onClick={() => setData({ ...data, languages: data.languages.includes(l.code) ? data.languages.filter(x => x !== l.code) : [...data.languages, l.code] })} className={`px-4 py-2 rounded-full border ${data.languages.includes(l.code) ? 'bg-purple-500 text-white' : 'border-gray-300'}`}>{l.name}</button>
                                        ))}
                                    </div>
                                </div>
                                <div><label className="block text-sm font-medium mb-2">Certifications</label>
                                    <div className="flex gap-2 mb-2">
                                        <input value={newCert} onChange={e => setNewCert(e.target.value)} placeholder="Ex: CFC" className="flex-1 px-4 py-2 border rounded-lg" onKeyPress={e => e.key === 'Enter' && newCert && (setData({ ...data, certifications: [...data.certifications, newCert] }), setNewCert(''))} />
                                        <button onClick={() => newCert && (setData({ ...data, certifications: [...data.certifications, newCert] }), setNewCert(''))} className="btn-secondary px-4"><Plus className="w-5 h-5" /></button>
                                    </div>
                                    <div className="flex flex-wrap gap-2">{data.certifications.map((c, i) => <span key={i} className="flex items-center gap-1 px-3 py-1 bg-gray-100 rounded-full text-sm">{c}<button onClick={() => setData({ ...data, certifications: data.certifications.filter((_, j) => j !== i) })} className="text-gray-500 hover:text-red-500"><X className="w-4 h-4" /></button></span>)}</div>
                                </div>
                            </div>
                        )}

                        {step === 2 && (
                            <div className="space-y-6">
                                <h2 className="text-xl font-semibold">Localisation & Tarifs</h2>
                                <div className="grid grid-cols-2 gap-4">
                                    <div><label className="block text-sm font-medium mb-2">Code postal *</label><input value={data.postalCode} onChange={e => setData({ ...data, postalCode: e.target.value })} className="w-full px-4 py-3 border rounded-lg" /></div>
                                    <div><label className="block text-sm font-medium mb-2">Ville *</label><input value={data.city} onChange={e => setData({ ...data, city: e.target.value })} className="w-full px-4 py-3 border rounded-lg" /></div>
                                </div>
                                <div><label className="block text-sm font-medium mb-2">Rayon: {data.serviceRadius} km</label><input type="range" min="5" max="100" value={data.serviceRadius} onChange={e => setData({ ...data, serviceRadius: +e.target.value })} className="w-full" /></div>
                                <div><label className="block text-sm font-medium mb-2">Tarif horaire (CHF) *</label><input type="number" value={data.hourlyRate} onChange={e => setData({ ...data, hourlyRate: +e.target.value })} className="w-full px-4 py-3 border rounded-lg" /></div>
                            </div>
                        )}

                        {step === 3 && (
                            <div className="space-y-6">
                                <h2 className="text-xl font-semibold">Vérification</h2>
                                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-blue-700 text-sm">La vérification est optionnelle mais recommandée pour obtenir le badge Vérifié.</div>
                                <div><label className="block text-sm font-medium mb-2">Pièce d&apos;identité</label><div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-purple-400"><Upload className="w-10 h-10 text-gray-400 mx-auto mb-2" /><p className="text-gray-600">Cliquez pour télécharger</p></div></div>
                                <div className="bg-gray-50 rounded-lg p-6"><h3 className="font-semibold mb-4">Récapitulatif</h3><p className="text-sm"><b>Catégories:</b> {data.categories.length} | <b>Langues:</b> {data.languages.join(', ')} | <b>Tarif:</b> {data.hourlyRate} CHF/h</p></div>
                            </div>
                        )}

                        <div className="flex justify-between mt-8 pt-6 border-t">
                            <button onClick={() => setStep(step - 1)} disabled={step === 0} className={step === 0 ? 'text-gray-400' : 'text-gray-600 hover:text-gray-900'}><ChevronLeft className="w-5 h-5 inline" /> Précédent</button>
                            {step < 3 ? <button onClick={() => setStep(step + 1)} disabled={!isValid()} className="btn-primary">Continuer <ChevronRight className="w-5 h-5 inline" /></button> : <button onClick={handleSubmit} disabled={loading} className="btn-primary">{loading ? 'Création...' : 'Créer mon profil'}</button>}
                        </div>
                    </div>
                </div>
            </main>
            <Footer /></>
    );
}
