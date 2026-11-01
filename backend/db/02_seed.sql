-- Données de démonstration (mêmes produits que MockPostgresBackendService de l'app Android).
-- Les vendeurs de démo ne correspondent à aucun compte Firebase : ils servent uniquement de catalogue.

INSERT INTO categories (id, name, description, icon_name) VALUES
    ('cat_furniture',   'Mobilier & Déco',   'Meubles et objets de décoration scannables en 3D', 'ic_furniture'),
    ('cat_electronics', 'Électronique',      'Appareils, gadgets et accessoires',                'ic_electronics'),
    ('cat_fashion',     'Mode & Chaussures', 'Vêtements, sacs et sneakers avec essayage AR',     'ic_fashion'),
    ('cat_art',         'Art & Sculptures',  'Objets uniques numérisés par SAM',                 'ic_art')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, name, email, city, whatsapp, phone_number) VALUES
    ('seller_101', 'Atelier Nordique',   'demo.atelier@example.invalid',   'Antananarivo', '+261340000101', '+261340000101'),
    ('seller_102', 'Luminaires & Co',    'demo.luminaires@example.invalid','Antananarivo', '+261340000102', '+261340000102'),
    ('seller_103', 'SneakersHub',        'demo.sneakers@example.invalid',  'Toamasina',    '+261340000103', '+261340000103')
ON CONFLICT (id) DO NOTHING;

INSERT INTO products (id, title, description, price, status, category_id, seller_id,
                      thumbnail_url, model3d_url, image_urls, stock, created_at) VALUES
    ('prod_1', 'Fauteuil Scandinave Velours',
     'Fauteuil confortable avec structure bois massif. Modèle 3D reconstruit par IA Meta SAM.',
     189.99, 'DISPONIBLE', 'cat_furniture', 'seller_101',
     'https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique',
     'https://modelviewer.dev/shared-assets/models/Astronaut.glb',
     ARRAY['https://via.placeholder.com/400x300.png?text=Fauteuil+Nordique'], 5, now() - interval '1 day'),
    ('prod_2', 'Lampe Design Industrielle',
     'Lampe à poser en acier brossé. Scannez et placez la lampe directement sur votre table en AR.',
     59.00, 'NOUVEAU', 'cat_furniture', 'seller_102',
     'https://via.placeholder.com/400x300.png?text=Lampe+Design',
     'https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/BoomBox/glTF-Binary/BoomBox.glb',
     ARRAY['https://via.placeholder.com/400x300.png?text=Lampe+Design'], 12, now() - interval '12 hours'),
    ('prod_3', 'Sneakers Édition Limitée',
     'Paire de sneakers exclusives. Reconstitution 3D photoréaliste via Meta SAM.',
     220.00, 'A_VENDRE', 'cat_fashion', 'seller_103',
     'https://via.placeholder.com/400x300.png?text=Sneakers+3D',
     'https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Models/master/2.0/Duck/glTF-Binary/Duck.glb',
     ARRAY['https://via.placeholder.com/400x300.png?text=Sneakers+3D'], 2, now() - interval '5 hours')
ON CONFLICT (id) DO NOTHING;
