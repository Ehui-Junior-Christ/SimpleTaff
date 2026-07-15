UPDATE utilisateur 
SET mot_de_passe_hash = '$2a$10$F0qnAsJML5OdYsYuYhe5OudXyhBbEfqBLOpOZ2VB7TCmKyVa76vau'
WHERE email IN (
    'superadmin@simpletaff.com',
    'admin@simpletaff.com',
    'coord@simpletaff.com',
    'employeur@simpletaff.com'
);
