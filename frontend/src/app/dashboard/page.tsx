'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Header from '@/components/Header';
import { useAuthStore } from '@/lib/store';
import { bookingApi, Booking } from '@/lib/api';
import {
    formatDate,
    getBookingStatusLabel,
    getBookingStatusColor,
    getCategoryIcon
} from '@/lib/utils';
import {
    Plus,
    Calendar,
    MessageCircle,
    Clock,
    ChevronRight,
    Search,
    Filter
} from 'lucide-react';

export default function DashboardPage() {
    const router = useRouter();
    const { user, isAuthenticated } = useAuthStore();
    const [bookings, setBookings] = useState<Booking[]>([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState<'all' | 'active' | 'completed'>('all');

    useEffect(() => {
        if (!isAuthenticated) {
            router.push('/login');
            return;
        }
        fetchBookings();
    }, [isAuthenticated]);

    const fetchBookings = async () => {
        try {
            const data = await bookingApi.getClientBookings();
            setBookings(data);
        } catch (error) {
            console.error('Failed to fetch bookings:', error);
        } finally {
            setLoading(false);
        }
    };

    const filteredBookings = bookings.filter((b) => {
        if (filter === 'active') {
            return ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'].includes(b.status);
        }
        if (filter === 'completed') {
            return ['COMPLETED', 'CANCELED', 'DECLINED'].includes(b.status);
        }
        return true;
    });

    const activeCount = bookings.filter((b) =>
        ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'].includes(b.status)
    ).length;

    if (!isAuthenticated) {
        return null;
    }

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
                                    Bonjour, {user?.name?.split(' ')[0]} 👋
                                </h1>
                                <p className="text-white/80">
                                    {activeCount > 0
                                        ? `Vous avez ${activeCount} demande${activeCount > 1 ? 's' : ''} en cours`
                                        : 'Bienvenue sur votre espace personnel'}
                                </p>
                            </div>
                            <Link
                                href="/book"
                                className="inline-flex items-center gap-2 px-6 py-3 rounded-xl bg-white text-violet-600 font-semibold shadow-lg hover:bg-gray-50 transition-colors"
                            >
                                <Plus className="w-5 h-5" />
                                Nouvelle demande
                            </Link>
                        </div>
                    </div>

                    {/* Stats Cards */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
                        {[
                            {
                                label: 'Demandes totales',
                                value: bookings.length,
                                icon: <Calendar className="w-5 h-5" />,
                                color: 'bg-blue-500'
                            },
                            {
                                label: 'En cours',
                                value: activeCount,
                                icon: <Clock className="w-5 h-5" />,
                                color: 'bg-orange-500'
                            },
                            {
                                label: 'Messages non lus',
                                value: bookings.reduce((acc, b) => acc + (b.unreadMessageCount || 0), 0),
                                icon: <MessageCircle className="w-5 h-5" />,
                                color: 'bg-green-500'
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

                    {/* Bookings List */}
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
                        <div className="p-4 md:p-6 border-b border-gray-100">
                            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                                <h2 className="text-xl font-bold text-gray-900">Mes demandes</h2>

                                {/* Filter Tabs */}
                                <div className="flex p-1 bg-gray-100 rounded-lg">
                                    {(['all', 'active', 'completed'] as const).map((f) => (
                                        <button
                                            key={f}
                                            onClick={() => setFilter(f)}
                                            className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${filter === f
                                                    ? 'bg-white text-gray-900 shadow-sm'
                                                    : 'text-gray-500 hover:text-gray-700'
                                                }`}
                                        >
                                            {f === 'all' ? 'Toutes' : f === 'active' ? 'En cours' : 'Terminées'}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>

                        {loading ? (
                            <div className="flex justify-center py-12">
                                <div className="spinner"></div>
                            </div>
                        ) : filteredBookings.length === 0 ? (
                            <div className="text-center py-12 px-4">
                                <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4">
                                    <Search className="w-8 h-8 text-gray-400" />
                                </div>
                                <h3 className="text-lg font-semibold text-gray-900 mb-2">
                                    Aucune demande
                                </h3>
                                <p className="text-gray-500 mb-6">
                                    {filter === 'all'
                                        ? "Vous n'avez pas encore fait de demande de service."
                                        : `Aucune demande ${filter === 'active' ? 'en cours' : 'terminée'}.`}
                                </p>
                                <Link
                                    href="/book"
                                    className="inline-flex items-center gap-2 px-6 py-3 rounded-xl btn-primary font-semibold"
                                >
                                    <Plus className="w-5 h-5" />
                                    Faire une demande
                                </Link>
                            </div>
                        ) : (
                            <div className="divide-y divide-gray-100">
                                {filteredBookings.map((booking) => (
                                    <Link
                                        key={booking.id}
                                        href={`/dashboard/bookings/${booking.id}`}
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
                                                        {booking.unreadMessageCount} nouveau{booking.unreadMessageCount > 1 ? 'x' : ''}
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-500 truncate">
                                                {booking.city} • {formatDate(booking.createdAt)}
                                            </p>
                                            {booking.provider && (
                                                <p className="text-sm text-violet-600 mt-1">
                                                    Prestataire : {booking.provider.name}
                                                </p>
                                            )}
                                        </div>

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
