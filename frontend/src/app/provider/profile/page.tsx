'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Mail, Phone, MapPin, Clock, Star, Edit2, Save, Plus, X, Check } from 'lucide-react';
import { useAuthStore } from '@/lib/store';
import { categoryApi, providerApi, Category } from '@/lib/api';
import { formatCurrency } from '@/lib/utils';
import Header from '@/components/Header';
import Footer from '@/components/Footer';

const LANGUAGES = [
    { code: 'fr', name: 'Français' },
    { code: 'de', name: 'Allemand' },
    { code: 'it', name: 'Italien' },
    { code: 'en', name: 'Anglais' },
];

interface ProviderProfile {
    id: string;
    bio: string;
    categoryIds: string[];
    languages: string[];
    postalCode: string;
    city: string;
    serviceRadius: number;
    hourlyRate: number;
    certifications: string[];
    yearsOfExperience: number;
    isVerified: boolean;
    averageRating: number;
    totalReviews: number;
}

export default function ProviderProfilePage() {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [profile, setProfile] = useState<ProviderProfile | null>(null);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [editing, setEditing] = useState(false);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [newCert, setNewCert] = useState('');

    const [formData, setFormData] = useState({
        bio: '',
        categoryIds: [] as string[],
        languages: [] as string[],
        postalCode: '',
        city: '',
        serviceRadius: 20,
        hourlyRate: 30,
        certifications: [] as string[],
        yearsOfExperience: 0,
    });

    useEffect(() => {
        if (!isAuthenticated) { router.push('/login'); return; }
        if (user?.role !== 'PROVIDER') { router.push('/dashboard'); return; }
        fetchData();
    }, [isAuthenticated, user, router]);

    const fetchData = async () => {
        try {
            const [profileData, categoriesData] = await Promise.all([
                providerApi.getProfile(),
                categoryApi.getAll()
            ]);
            setProfile(profileData);
            setCategories(categoriesData);
            setFormData({
                bio: profileData.bio,
                categoryIds: profileData.categoryIds,
                languages: profileData.languages,
                postalCode: profileData.postalCode,
                city: profileData.city,
                serviceRadius: profileData.serviceRadius,
                hourlyRate: profileData.hourlyRate,
                certifications: profileData.certifications,
                yearsOfExperience: profileData.yearsOfExperience,
            });
        } catch (err) {
            console.error('Failed to fetch profile:', err);
            // If no profile exists, redirect to onboarding
            router.push('/provider/onboarding');
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        setError('');
        setSuccess('');
        try {
            await providerApi.updateProfile(formData);
            setSuccess('Profil mis à jour avec succès');
            setEditing(false);
            fetchData();
        } catch (err: unknown) {
            const error = err as { response?: { data?: { message?: string } } };
            setError(error.response?.data?.message || 'Erreur lors de la sauvegarde');
        } finally {
            setSaving(false);
        }
    };

    const getCategoryName = (id: string) => categories.find(c => c.id === id)?.name || '';

    if (loading) {
        return (
            <><Header /><main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-purple-50 pt-24">
                <div className="container mx-auto px-4 py-8"><div className="flex items-center justify-center h-64"><div className="spinner"></div></div></div>
            </main><Footer /></>
        );
    }

    return (
        <><Header />
            <main className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-purple-50 pt-24">
                <div className="container mx-auto px-4 py-8 max-w-4xl">
                    <div className="flex items-center justify-between mb-8">
                        <div>
                            <h1 className="text-3xl font-bold text-gray-900">Mon profil prestataire</h1>
                            <p className="text-gray-600 mt-1">Gérez vos informations et paramètres</p>
                        </div>
                        {!editing ? (
                            <button onClick={() => setEditing(true)} className="btn-secondary flex items-center gap-2">
                                <Edit2 className="w-4 h-4" /> Modifier
                            </button>
                        ) : (
                            <div className="flex gap-2">
                                <button onClick={() => setEditing(false)} className="btn-secondary">Annuler</button>
                                <button onClick={handleSave} disabled={saving} className="btn-primary flex items-center gap-2">
                                    {saving ? 'Sauvegarde...' : <><Save className="w-4 h-4" /> Sauvegarder</>}
                                </button>
                            </div>
                        )}
                    </div>

                    {error && <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-600">{error}</div>}
                    {success && <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg text-green-600 flex items-center gap-2"><Check className="w-5 h-5" />{success}</div>}

                    <div className="grid gap-6">
                        {/* Profile Header */}
                        <div className="glass-effect rounded-2xl p-6">
                            <div className="flex items-start gap-6">
                                <div className="relative">
                                    <div className="w-24 h-24 bg-gradient-to-br from-purple-500 to-indigo-600 rounded-full flex items-center justify-center text-white text-2xl font-bold">
                                        {user?.firstName?.[0]}{user?.lastName?.[0]}
                                    </div>
                                    {profile?.isVerified && (
                                        <div className="absolute -bottom-1 -right-1 w-8 h-8 bg-green-500 rounded-full flex items-center justify-center border-2 border-white">
                                            <Check className="w-4 h-4 text-white" />
                                        </div>
                                    )}
                                </div>
                                <div className="flex-1">
                                    <h2 className="text-2xl font-bold text-gray-900">{user?.firstName} {user?.lastName}</h2>
                                    <div className="flex items-center gap-4 mt-2 text-gray-600">
                                        <span className="flex items-center gap-1"><Mail className="w-4 h-4" />{user?.email}</span>
                                        {user?.phone && <span className="flex items-center gap-1"><Phone className="w-4 h-4" />{user.phone}</span>}
                                    </div>
                                    <div className="flex items-center gap-4 mt-3">
                                        {profile?.isVerified && <span className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">Vérifié</span>}
                                        <span className="flex items-center gap-1 text-yellow-600"><Star className="w-4 h-4 fill-current" />{profile?.averageRating || 'N/A'} ({profile?.totalReviews || 0} avis)</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Bio */}
                        <div className="glass-effect rounded-2xl p-6">
                            <h3 className="text-lg font-semibold text-gray-900 mb-4">À propos</h3>
                            {editing ? (
                                <textarea value={formData.bio} onChange={e => setFormData({ ...formData, bio: e.target.value })} className="w-full px-4 py-3 border rounded-lg" rows={4} placeholder="Décrivez-vous..." />
                            ) : (
                                <p className="text-gray-600">{profile?.bio || 'Aucune description'}</p>
                            )}
                        </div>

                        {/* Services & Languages */}
                        <div className="grid md:grid-cols-2 gap-6">
                            <div className="glass-effect rounded-2xl p-6">
                                <h3 className="text-lg font-semibold text-gray-900 mb-4">Services proposés</h3>
                                {editing ? (
                                    <div className="grid grid-cols-2 gap-2">
                                        {categories.map(c => (
                                            <button key={c.id} onClick={() => setFormData({ ...formData, categoryIds: formData.categoryIds.includes(c.id) ? formData.categoryIds.filter(x => x !== c.id) : [...formData.categoryIds, c.id] })} className={`p-3 rounded-lg border text-left text-sm ${formData.categoryIds.includes(c.id) ? 'border-purple-500 bg-purple-50' : 'border-gray-200'}`}>{c.name}</button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="flex flex-wrap gap-2">
                                        {profile?.categoryIds.map(id => <span key={id} className="px-3 py-1 bg-purple-100 text-purple-700 rounded-full text-sm">{getCategoryName(id)}</span>)}
                                    </div>
                                )}
                            </div>
                            <div className="glass-effect rounded-2xl p-6">
                                <h3 className="text-lg font-semibold text-gray-900 mb-4">Langues</h3>
                                {editing ? (
                                    <div className="flex flex-wrap gap-2">
                                        {LANGUAGES.map(l => (
                                            <button key={l.code} onClick={() => setFormData({ ...formData, languages: formData.languages.includes(l.code) ? formData.languages.filter(x => x !== l.code) : [...formData.languages, l.code] })} className={`px-4 py-2 rounded-full border ${formData.languages.includes(l.code) ? 'bg-purple-500 text-white' : 'border-gray-300'}`}>{l.name}</button>
                                        ))}
                                    </div>
                                ) : (
                                    <div className="flex flex-wrap gap-2">
                                        {profile?.languages.map(code => <span key={code} className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm">{LANGUAGES.find(l => l.code === code)?.name || code}</span>)}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Location & Pricing */}
                        <div className="grid md:grid-cols-2 gap-6">
                            <div className="glass-effect rounded-2xl p-6">
                                <h3 className="text-lg font-semibold text-gray-900 mb-4"><MapPin className="w-5 h-5 inline mr-2" />Localisation</h3>
                                {editing ? (
                                    <div className="space-y-4">
                                        <div className="grid grid-cols-2 gap-4">
                                            <div><label className="block text-sm font-medium mb-1">Code postal</label><input value={formData.postalCode} onChange={e => setFormData({ ...formData, postalCode: e.target.value })} className="w-full px-3 py-2 border rounded-lg" /></div>
                                            <div><label className="block text-sm font-medium mb-1">Ville</label><input value={formData.city} onChange={e => setFormData({ ...formData, city: e.target.value })} className="w-full px-3 py-2 border rounded-lg" /></div>
                                        </div>
                                        <div><label className="block text-sm font-medium mb-1">Rayon: {formData.serviceRadius} km</label><input type="range" min="5" max="100" value={formData.serviceRadius} onChange={e => setFormData({ ...formData, serviceRadius: +e.target.value })} className="w-full" /></div>
                                    </div>
                                ) : (
                                    <div className="space-y-2 text-gray-600">
                                        <p>{profile?.postalCode} {profile?.city}</p>
                                        <p>Rayon d&apos;intervention: {profile?.serviceRadius} km</p>
                                    </div>
                                )}
                            </div>
                            <div className="glass-effect rounded-2xl p-6">
                                <h3 className="text-lg font-semibold text-gray-900 mb-4"><Clock className="w-5 h-5 inline mr-2" />Tarifs & Expérience</h3>
                                {editing ? (
                                    <div className="space-y-4">
                                        <div><label className="block text-sm font-medium mb-1">Tarif horaire (CHF)</label><input type="number" value={formData.hourlyRate} onChange={e => setFormData({ ...formData, hourlyRate: +e.target.value })} className="w-full px-3 py-2 border rounded-lg" /></div>
                                        <div><label className="block text-sm font-medium mb-1">Années d&apos;expérience</label><input type="number" value={formData.yearsOfExperience} onChange={e => setFormData({ ...formData, yearsOfExperience: +e.target.value })} className="w-full px-3 py-2 border rounded-lg" /></div>
                                    </div>
                                ) : (
                                    <div className="space-y-2 text-gray-600">
                                        <p className="text-2xl font-bold text-gray-900">{formatCurrency(profile?.hourlyRate || 0)}/h</p>
                                        <p>{profile?.yearsOfExperience} ans d&apos;expérience</p>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Certifications */}
                        <div className="glass-effect rounded-2xl p-6">
                            <h3 className="text-lg font-semibold text-gray-900 mb-4">Certifications</h3>
                            {editing ? (
                                <div>
                                    <div className="flex gap-2 mb-3">
                                        <input value={newCert} onChange={e => setNewCert(e.target.value)} placeholder="Ajouter une certification" className="flex-1 px-3 py-2 border rounded-lg" onKeyPress={e => e.key === 'Enter' && newCert && (setFormData({ ...formData, certifications: [...formData.certifications, newCert] }), setNewCert(''))} />
                                        <button onClick={() => newCert && (setFormData({ ...formData, certifications: [...formData.certifications, newCert] }), setNewCert(''))} className="btn-secondary"><Plus className="w-5 h-5" /></button>
                                    </div>
                                    <div className="flex flex-wrap gap-2">{formData.certifications.map((c, i) => <span key={i} className="flex items-center gap-1 px-3 py-1 bg-gray-100 rounded-full text-sm">{c}<button onClick={() => setFormData({ ...formData, certifications: formData.certifications.filter((_, j) => j !== i) })} className="text-gray-500 hover:text-red-500"><X className="w-4 h-4" /></button></span>)}</div>
                                </div>
                            ) : (
                                <div className="flex flex-wrap gap-2">
                                    {profile?.certifications?.length ? profile.certifications.map((c, i) => <span key={i} className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm">{c}</span>) : <p className="text-gray-500">Aucune certification</p>}
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </main>
            <Footer /></>
    );
}
