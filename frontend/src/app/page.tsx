'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import { categoryApi, Category } from '@/lib/api';
import { getCategoryIcon } from '@/lib/utils';
import { ArrowRight, Star, Shield, Clock, Users, CheckCircle } from 'lucide-react';

export default function HomePage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchCategories() {
      try {
        const data = await categoryApi.getAll();
        setCategories(data);
      } catch (error) {
        console.error('Failed to fetch categories:', error);
      } finally {
        setLoading(false);
      }
    }
    fetchCategories();
  }, []);

  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      {/* Hero Section */}
      <section className="hero-gradient bg-pattern pt-24 pb-16 md:pt-32 md:pb-24">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/80 backdrop-blur-sm border border-violet-100 mb-6">
              <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse"></span>
              <span className="text-sm font-medium text-gray-700">Nouveau : Vérification des prestataires renforcée</span>
            </div>

            <h1 className="text-4xl md:text-6xl font-bold text-gray-900 leading-tight mb-6">
              Trouvez le{' '}
              <span className="gradient-text">service parfait</span>{' '}
              près de chez vous
            </h1>

            <p className="text-lg md:text-xl text-gray-600 mb-8 leading-relaxed">
              Babysitting, bricolage, aide administrative...
              Connectez-vous avec des prestataires de confiance en quelques clics.
            </p>

            {/* Search Box */}
            <div className="bg-white rounded-2xl shadow-xl p-4 md:p-6 max-w-2xl mx-auto">
              <div className="flex flex-col md:flex-row gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-2 text-left">
                    Quel service recherchez-vous ?
                  </label>
                  <select className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all">
                    <option value="">Choisir un service</option>
                    {categories.map((cat) => (
                      <option key={cat.id} value={cat.id}>
                        {getCategoryIcon(cat.icon)} {cat.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-2 text-left">
                    Où ?
                  </label>
                  <input
                    type="text"
                    placeholder="Code postal ou ville"
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:border-violet-400 focus:ring-2 focus:ring-violet-100 transition-all"
                  />
                </div>
                <div className="flex items-end">
                  <Link
                    href="/book"
                    className="w-full md:w-auto px-8 py-3 rounded-xl btn-primary font-semibold flex items-center justify-center gap-2"
                  >
                    Rechercher
                    <ArrowRight className="w-5 h-5" />
                  </Link>
                </div>
              </div>
            </div>

            {/* Trust indicators */}
            <div className="flex flex-wrap justify-center gap-8 mt-10 text-sm text-gray-500">
              <div className="flex items-center gap-2">
                <CheckCircle className="w-5 h-5 text-green-500" />
                <span>Prestataires vérifiés</span>
              </div>
              <div className="flex items-center gap-2">
                <Shield className="w-5 h-5 text-blue-500" />
                <span>Paiement sécurisé</span>
              </div>
              <div className="flex items-center gap-2">
                <Star className="w-5 h-5 text-yellow-500" />
                <span>4.8/5 de satisfaction</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Categories Section */}
      <section className="py-16 md:py-24 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Nos catégories de services
            </h2>
            <p className="text-gray-600 max-w-2xl mx-auto">
              Découvrez nos 7 catégories de services pour répondre à tous vos besoins du quotidien.
            </p>
          </div>

          {loading ? (
            <div className="flex justify-center py-12">
              <div className="spinner"></div>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {categories.map((category) => (
                <Link
                  key={category.id}
                  href={`/book?category=${category.slug}`}
                  className="category-card group relative z-10"
                >
                  <div className="relative z-10">
                    <div className="text-4xl mb-4">{getCategoryIcon(category.icon)}</div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2 group-hover:text-violet-600 transition-colors">
                      {category.name}
                    </h3>
                    <p className="text-sm text-gray-500 line-clamp-2">
                      {category.description}
                    </p>
                  </div>
                  <ArrowRight className="absolute bottom-4 right-4 w-5 h-5 text-violet-400 opacity-0 group-hover:opacity-100 transition-all transform group-hover:translate-x-1" />
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      {/* Featured Providers Section */}
      <section className="py-16 md:py-24 bg-gradient-to-b from-white to-violet-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Nos prestataires à la une
            </h2>
            <p className="text-gray-600 max-w-2xl mx-auto">
              Découvrez quelques-uns de nos prestataires les mieux notés, prêts à vous aider.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {[
              {
                name: 'Marie Bernard',
                specialty: 'Garde d\'enfants',
                rating: 4.9,
                reviews: 127,
                hourlyRate: 18,
                avatar: '👩‍🏫',
                verified: true,
                bio: 'Éducatrice diplômée avec 8 ans d\'expérience. Spécialisée dans l\'éveil et les activités créatives.',
                location: 'Genève',
              },
              {
                name: 'Thomas Müller',
                specialty: 'Bricolage & Réparations',
                rating: 4.8,
                reviews: 89,
                hourlyRate: 35,
                avatar: '👨‍🔧',
                verified: true,
                bio: 'Artisan polyvalent, je réalise tous vos travaux de plomberie, électricité et menuiserie.',
                location: 'Lausanne',
              },
              {
                name: 'Sophie Laurent',
                specialty: 'Aide administrative',
                rating: 5.0,
                reviews: 64,
                hourlyRate: 40,
                avatar: '👩‍💼',
                verified: true,
                bio: 'Ancienne assistante de direction, j\'aide particuliers et PME dans leurs démarches administratives.',
                location: 'Zürich',
              },
              {
                name: 'Pierre Dubois',
                specialty: 'Jardinage',
                rating: 4.7,
                reviews: 52,
                hourlyRate: 28,
                avatar: '👨‍🌾',
                verified: false,
                bio: 'Passionné de jardinage, je m\'occupe de l\'entretien de vos espaces verts toute l\'année.',
                location: 'Berne',
              },
              {
                name: 'Anna Rossi',
                specialty: 'Cours particuliers',
                rating: 4.9,
                reviews: 103,
                hourlyRate: 45,
                avatar: '👩‍🎓',
                verified: true,
                bio: 'Professeure certifiée, j\'accompagne vos enfants en maths, français et anglais du primaire au lycée.',
                location: 'Lugano',
              },
              {
                name: 'Marc Weber',
                specialty: 'Assistance informatique',
                rating: 4.8,
                reviews: 78,
                hourlyRate: 50,
                avatar: '👨‍💻',
                verified: true,
                bio: 'Ingénieur IT, je dépanne vos ordinateurs, configure vos réseaux et sécurise vos données.',
                location: 'Bâle',
              },
            ].map((provider, index) => (
              <div
                key={index}
                className="provider-card bg-white rounded-2xl p-6 shadow-lg hover:shadow-xl transition-all duration-300 card-hover"
              >
                <div className="flex items-start gap-4 mb-4">
                  <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-violet-100 to-pink-100 flex items-center justify-center text-3xl">
                    {provider.avatar}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h3 className="text-lg font-semibold text-gray-900">{provider.name}</h3>
                      {provider.verified && (
                        <span className="verified-badge text-xs">
                          <Shield className="w-3 h-3" />
                          Vérifié
                        </span>
                      )}
                    </div>
                    <p className="text-sm text-violet-600 font-medium">{provider.specialty}</p>
                    <p className="text-xs text-gray-400">{provider.location}</p>
                  </div>
                </div>

                <p className="text-sm text-gray-600 mb-4 line-clamp-2">{provider.bio}</p>

                <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                  <div className="flex items-center gap-1">
                    <Star className="w-4 h-4 text-yellow-400 fill-yellow-400" />
                    <span className="font-semibold text-gray-900">{provider.rating}</span>
                    <span className="text-sm text-gray-400">({provider.reviews} avis)</span>
                  </div>
                  <div className="text-right">
                    <span className="text-lg font-bold text-gray-900">{provider.hourlyRate} CHF</span>
                    <span className="text-sm text-gray-400">/h</span>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="text-center mt-12">
            <Link
              href="/book"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-full btn-secondary font-semibold text-lg"
            >
              Voir tous les prestataires
              <ArrowRight className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="py-16 md:py-24 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Comment ça marche ?
            </h2>
            <p className="text-gray-600 max-w-2xl mx-auto">
              En 3 étapes simples, trouvez le prestataire idéal pour votre besoin.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {[
              {
                step: 1,
                icon: <Users className="w-8 h-8" />,
                title: 'Décrivez votre besoin',
                description: 'Sélectionnez le type de service, votre localisation et vos préférences.',
              },
              {
                step: 2,
                icon: <Star className="w-8 h-8" />,
                title: 'Choisissez un prestataire',
                description: 'Consultez les profils, avis et tarifs pour faire le meilleur choix.',
              },
              {
                step: 3,
                icon: <Clock className="w-8 h-8" />,
                title: 'Réservez en toute confiance',
                description: 'Confirmez la réservation et communiquez directement avec votre prestataire.',
              },
            ].map((item) => (
              <div key={item.step} className="text-center">
                <div className="relative inline-block mb-6">
                  <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-violet-500 to-pink-500 flex items-center justify-center text-white">
                    {item.icon}
                  </div>
                  <div className="absolute -top-2 -right-2 w-8 h-8 rounded-full bg-white shadow-lg flex items-center justify-center text-violet-600 font-bold text-sm">
                    {item.step}
                  </div>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">{item.title}</h3>
                <p className="text-gray-500">{item.description}</p>
              </div>
            ))}
          </div>

          <div className="text-center mt-12">
            <Link
              href="/book"
              className="inline-flex items-center gap-2 px-8 py-4 rounded-full btn-primary font-semibold text-lg"
            >
              Commencer maintenant
              <ArrowRight className="w-5 h-5" />
            </Link>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 md:py-24 bg-gradient-to-r from-violet-600 to-pink-600">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Vous êtes un professionnel ?
          </h2>
          <p className="text-lg text-white/80 max-w-2xl mx-auto mb-8">
            Rejoignez notre réseau de prestataires vérifiés et développez votre activité.
            Inscription gratuite et sans engagement.
          </p>
          <Link
            href="/register?provider=true"
            className="inline-flex items-center gap-2 px-8 py-4 rounded-full bg-white text-violet-600 font-semibold text-lg hover:bg-gray-100 transition-colors shadow-xl"
          >
            Devenir prestataire
            <ArrowRight className="w-5 h-5" />
          </Link>
        </div>
      </section>

      <Footer />
    </div>
  );
}
