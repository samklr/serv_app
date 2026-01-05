'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import { useAuthStore } from '@/lib/store';
import { bookingApi, Booking } from '@/lib/api';
import {
    formatDate,
    formatDateTime,
    getBookingStatusLabel,
    getBookingStatusColor,
    getCategoryIcon,
    formatCurrency
} from '@/lib/utils';
import {
    Check,
    X,
    Calendar,
    MessageCircle,
    Clock,
    ChevronRight,
    MapPin,
    Loader2,
    Bell,
    TrendingUp,
    Users
} from 'lucide-react';

export default function ProviderDashboardPage() {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [pendingBookings, setPendingBookings] = useState<Booking[]>([]);
    const [allBookings, setAllBookings] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState<string | null>(null);

    useEffect(() => {
        if (!isAuthenticated || user?.role !== 'PROVIDER') {
            router.push('/login');
            return;
        }
        fetchBookings();
    }, [isAuthenticated, user]);

    const fetchBookings = async () => {
        try {
            const [pending, all] = await Promise.all([
                bookingApi.getPendingRequests(),
                bookingApi.getProviderBookings(),
            ]);
            setPendingBookings(pending);
            setAllBookings(all);
        } catch (error) {
            console.error('Failed to fetch bookings:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleAccept = async (id: string) => {
        setActionLoading(id);
        try {
            await bookingApi.accept(id);
            fetchBookings();
        } catch (error) {
            console.error('Failed to accept:', error);
        } finally {
            setActionLoading(null);
        }
    };

    const handleDecline = async (id: string) => {
        const reason = prompt('Raison du refus (optionnel):');
        setActionLoading(id);
        try {
            await bookingApi.decline(id, reason || undefined);
            fetchBookings();
        } catch (error) {
            console.error('Failed to decline:', error);
        } finally {
            setActionLoading(null);
        }
    };

    const handleComplete = async (id: string) => {
        if (!confirm('Marquer cette prestation comme terminée ?')) return;
        setActionLoading(id);
        try {
            await bookingApi.complete(id);
            fetchBookings();
        } catch (error) {
            console.error('Failed to complete:', error);
        } finally {
            setActionLoading(null);
        }
    };

    if (!isAuthenticated || user?.role !== 'PROVIDER') {
        return null;
    }

    const activeBookings = allBookings.filter((b) =>
        ['ACCEPTED', 'IN_PROGRESS'].includes(b.status)
    );
    const completedCount = allBookings.filter((b) => b.status === 'COMPLETED').length;

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="pt-20 pb-12">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    {/* Welcome Section */}
                    <div className="bg-gradient-to-r from-violet-600 to-pink-600 rounded-2xl p-6 md:p-8 mb-8 text-white">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                            <div>
                                <h1 className="text-2xl md:text-3xl font-bold mb-2">
                                    Espace Prestataire
                                </h1>
                                <p className="text-white/80">
                                    {pendingBookings.length > 0
                                        ? `Vous avez ${pendingBookings.length} nouvelle${pendingBookings.length > 1 ? 's' : ''} demande${pendingBookings.length > 1 ? 's' : ''}`
                                        : 'Bienvenue sur votre espace prestataire'}
                                </p>
                            </div>
                            <Link
                                href="/provider/profile"
                                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-white text-violet-600 font-semibold shadow-lg hover:bg-gray-50 transition-colors"
                            >
                                Gérer mon profil
                            </Link>
                        </div>
                    </div>

                    {/* Stats Cards */}
                    <div className="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-8">
                        {[
                            {
                                label: 'Nouvelles demandes',
                                value: pendingBookings.length,
                                icon: <Bell className="w-5 h-5" />,
                                color: 'bg-orange-500'
                            },
                            {
                                label: 'En cours',
                                value: activeBookings.length,
                                icon: <Clock className="w-5 h-5" />,
                                color: 'bg-blue-500'
                            },
                            {
                                label: 'Terminées',
                                value: completedCount,
                                icon: <Check className="w-5 h-5" />,
                                color: 'bg-green-500'
                            },
                            {
                                label: 'Messages non lus',
                                value: allBookings.reduce((acc, b) => acc + (b.unreadMessageCount || 0), 0),
                                icon: <MessageCircle className="w-5 h-5" />,
                                color: 'bg-purple-500'
                            },
                        ].map((stat, i) => (
                            <div key={i} className="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                                <div className="flex items-center gap-4">
                                    <div className={`w-12 h-12 rounded-xl ${stat.color} flex items-center justify-center text-white`}>
                                        {stat.icon}
                                    </div>
                                    <div>
                                        <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                                        <p className="text-sm text-gray-500">{stat.label}</p>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Pending Requests */}
                    {pendingBookings.length > 0 && (
                        <div className="bg-white rounded-2xl shadow-sm border border-orange-200 mb-8">
                            <div className="p-4 md:p-6 bg-orange-50 border-b border-orange-200 rounded-t-2xl">
                                <div className="flex items-center gap-2">
                                    <Bell className="w-5 h-5 text-orange-500" />
                                    <h2 className="text-xl font-bold text-gray-900">Nouvelles demandes</h2>
                                    <span className="ml-2 px-2.5 py-0.5 rounded-full bg-orange-500 text-white text-sm font-medium">
                                        {pendingBookings.length}
                                    </span>
                                </div>
                            </div>

                            <div className="divide-y divide-gray-100">
                                {pendingBookings.map((booking) => (
                                    <div key={booking.id} className="p-4 md:p-6">
                                        <div className="flex flex-col md:flex-row md:items-start gap-4">
                                            <div className="w-12 h-12 rounded-xl bg-violet-100 flex items-center justify-center text-2xl shrink-0">
                                                {getCategoryIcon(booking.category.slug)}
                                            </div>

                                            <div className="flex-1 min-w-0">
                                                <h3 className="font-semibold text-gray-900 mb-1">
                                                    {booking.category.name}
                                                </h3>
                                                <p className="text-gray-600 text-sm mb-2 line-clamp-2">
                                                    {booking.description}
                                                </p>
                                                <div className="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                                                    <span className="flex items-center gap-1">
                                                        <MapPin className="w-4 h-4" />
                                                        {booking.postalCode} {booking.city}
                                                    </span>
                                                    <span className="flex items-center gap-1">
                                                        <Calendar className="w-4 h-4" />
                                                        {formatDate(booking.createdAt)}
                                                    </span>
                                                    {booking.budgetMax && (
                                                        <span className="font-medium text-violet-600">
                                                            Budget: {formatCurrency(booking.budgetMax)}
                                                        </span>
                                                    )}
                                                </div>
                                                <p className="text-sm text-gray-500 mt-2">
                                                    Client: {booking.client.name}
                                                </p>
                                            </div>

                                            <div className="flex gap-2">
                                                <button
                                                    onClick={() => handleDecline(booking.id)}
                                                    disabled={actionLoading === booking.id}
                                                    className="px-4 py-2 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 transition-colors flex items-center gap-1 disabled:opacity-50"
                                                >
                                                    {actionLoading === booking.id ? (
                                                        <Loader2 className="w-4 h-4 animate-spin" />
                                                    ) : (
                                                        <X className="w-4 h-4" />
                                                    )}
                                                    Refuser
                                                </button>
                                                <button
                                                    onClick={() => handleAccept(booking.id)}
                                                    disabled={actionLoading === booking.id}
                                                    className="px-4 py-2 rounded-lg bg-green-500 text-white hover:bg-green-600 transition-colors flex items-center gap-1 disabled:opacity-50"
                                                >
                                                    {actionLoading === booking.id ? (
                                                        <Loader2 className="w-4 h-4 animate-spin" />
                                                    ) : (
                                                        <Check className="w-4 h-4" />
                                                    )}
                                                    Accepter
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* Active Bookings */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
                        <div className="p-4 md:p-6 border-b border-gray-100">
                            <h2 className="text-xl font-bold text-gray-900">Mes prestations</h2>
                        </div>

                        {loading ? (
                            <div className="flex justify-center py-12">
                                <div className="spinner"></div>
                            </div>
                        ) : allBookings.length === 0 ? (
                            <div className="text-center py-12 px-4">
                                <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4">
                                    <Users className="w-8 h-8 text-gray-400" />
                                </div>
                                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                                    Aucune prestation
                                </h3>
                                <p className="text-gray-500">
                                    Vous n'avez pas encore de prestation. Les demandes arriveront bientôt !
                                </p>
                            </div>
                        ) : (
                            <div className="divide-y divide-gray-100">
                                {allBookings.map((booking) => (
                                    <Link
                                        key={booking.id}
                                        href={`/provider/bookings/${booking.id}`}
                                        className="flex items-center gap-4 p-4 md:p-6 hover:bg-gray-50 transition-colors group"
                                    >
                                        <div className="w-12 h-12 rounded-xl bg-violet-100 flex items-center justify-center text-2xl shrink-0">
                                            {getCategoryIcon(booking.category.slug)}
                                        </div>

                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2 mb-1">
                                                <h3 className="font-semibold text-gray-900 truncate">
                                                    {booking.category.name}
                                                </h3>
                                                <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${getBookingStatusColor(booking.status)}`}>
                                                    {getBookingStatusLabel(booking.status)}
                                                </span>
                                                {booking.unreadMessageCount > 0 && (
                                                    <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-500 text-white">
                                                        {booking.unreadMessageCount}
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-500 truncate">
                                                {booking.client.name} • {booking.city} • {formatDate(booking.createdAt)}
                                            </p>
                                        </div>

                                        {booking.status === 'ACCEPTED' && (
                                            <button
                                                onClick={(e) => {
                                                    e.preventDefault();
                                                    handleComplete(booking.id);
                                                }}
                                                disabled={actionLoading === booking.id}
                                                className="px-3 py-1.5 rounded-lg bg-green-100 text-green-700 hover:bg-green-200 transition-colors text-sm font-medium shrink-0"
                                            >
                                                Terminer
                                            </button>
                                        )}

                                        <ChevronRight className="w-5 h-5 text-gray-400 group-hover:text-violet-500 transition-colors shrink-0" />
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
