import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number, currency = 'CHF'): string {
    return new Intl.NumberFormat('fr-CH', {
        style: 'currency',
        currency,
    }).format(amount);
}

export function formatDate(dateString: string): string {
    return new Intl.DateTimeFormat('fr-CH', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
    }).format(new Date(dateString));
}

export function formatDateTime(dateString: string): string {
    return new Intl.DateTimeFormat('fr-CH', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    }).format(new Date(dateString));
}

export function getTimeSlotLabel(slot: string): string {
    const labels: Record<string, string> = {
        MORNING: 'Matin (8h-12h)',
        AFTERNOON: 'Après-midi (12h-17h)',
        EVENING: 'Soir (17h-21h)',
    };
    return labels[slot] || slot;
}

export function getBookingStatusLabel(status: string): string {
    const labels: Record<string, string> = {
        REQUESTED: 'En attente',
        ACCEPTED: 'Acceptée',
        DECLINED: 'Refusée',
        IN_PROGRESS: 'En cours',
        COMPLETED: 'Terminée',
        CANCELED: 'Annulée',
    };
    return labels[status] || status;
}

export function getBookingStatusColor(status: string): string {
    const colors: Record<string, string> = {
        REQUESTED: 'bg-yellow-100 text-yellow-800',
        ACCEPTED: 'bg-blue-100 text-blue-800',
        DECLINED: 'bg-red-100 text-red-800',
        IN_PROGRESS: 'bg-purple-100 text-purple-800',
        COMPLETED: 'bg-green-100 text-green-800',
        CANCELED: 'bg-gray-100 text-gray-800',
    };
    return colors[status] || 'bg-gray-100 text-gray-800';
}

export function getCategoryIcon(icon: string): string {
    const icons: Record<string, string> = {
        baby: '👶',
        wrench: '🔧',
        'heart-pulse': '❤️',
        'file-text': '📄',
        briefcase: '💼',
        plane: '✈️',
        'hand-helping': '🤝',
    };
    return icons[icon] || '📋';
}

export function getLanguageLabel(code: string): string {
    const labels: Record<string, string> = {
        fr: 'Français',
        de: 'Allemand',
        en: 'Anglais',
        it: 'Italien',
        pt: 'Portugais',
        es: 'Espagnol',
    };
    return labels[code] || code.toUpperCase();
}
