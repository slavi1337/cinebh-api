CREATE TYPE user_role AS ENUM ('CUSTOMER', 'EMPLOYEE', 'ADMIN');
CREATE TYPE movie_status AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');
CREATE TYPE seat_type AS ENUM ('REGULAR', 'LOVE', 'VIP');
CREATE TYPE booking_status AS ENUM ('HOLD', 'RESERVED', 'PAID', 'CANCELLED', 'EXPIRED');
CREATE TYPE verification_code_type AS ENUM ('ACCOUNT_VERIFICATION', 'PASSWORD_RESET');
CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'CANCELLED', 'REFUNDED');

CREATE TABLE cities (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        name varchar(100) NOT NULL,
                        country varchar(100) NOT NULL DEFAULT 'Bosnia and Herzegovina'
);

CREATE TABLE users (
                       id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       first_name varchar(50) NOT NULL,
                       last_name varchar(50) NOT NULL,
                       email varchar(255) UNIQUE NOT NULL,
                       password_hash varchar(255) NOT NULL,
                       phone varchar(50) UNIQUE NOT NULL,
                       profile_image_url varchar(255) NOT NULL,
                       role user_role DEFAULT 'CUSTOMER',
                       is_active boolean DEFAULT false,
                       city_id uuid NOT NULL REFERENCES cities(id),
                       street_address varchar(255) NOT NULL,
                       stripe_customer_id varchar(255) UNIQUE,
                       created_at timestamptz DEFAULT now(),
                       updated_at timestamptz
);

CREATE TABLE verification_codes (
                                    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    user_id uuid NOT NULL REFERENCES users(id),
                                    type verification_code_type NOT NULL,
                                    code_hash varchar(255) NOT NULL,
                                    expires_at timestamptz NOT NULL,
                                    is_used boolean DEFAULT false,
                                    created_at timestamptz DEFAULT now()
);

CREATE TABLE movies (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        title varchar(255) NOT NULL,
                        synopsis text,
                        duration_minutes integer,
                        pg_rating varchar(8),
                        language varchar(50),
                        trailer_url varchar(255),
                        status movie_status DEFAULT 'DRAFT',
                        draft_step integer DEFAULT 1,
                        release_date date,
                        end_date date,
                        created_at timestamptz DEFAULT now()
);

CREATE TABLE genres (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        name varchar(50) UNIQUE NOT NULL
);

CREATE TABLE movie_genres (
                              id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              movie_id uuid NOT NULL REFERENCES movies(id),
                              genre_id uuid NOT NULL REFERENCES genres(id),
                              UNIQUE (movie_id, genre_id)
);

CREATE TABLE people (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        first_name varchar(100) NOT NULL,
                        last_name varchar(100) NOT NULL
);

CREATE TABLE movie_cast (
                            id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            movie_id uuid NOT NULL REFERENCES movies(id),
                            person_id uuid NOT NULL REFERENCES people(id),
                            character_name varchar(100),
                            UNIQUE (movie_id, person_id)
);

CREATE TABLE movie_writers (
                               id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                               movie_id uuid NOT NULL REFERENCES movies(id),
                               person_id uuid NOT NULL REFERENCES people(id),
                               UNIQUE (movie_id, person_id)
);

CREATE TABLE movie_directors (
                                 id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                 movie_id uuid NOT NULL REFERENCES movies(id),
                                 person_id uuid NOT NULL REFERENCES people(id),
                                 UNIQUE (movie_id, person_id)
);

CREATE TABLE movie_images (
                              id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              movie_id uuid NOT NULL REFERENCES movies(id),
                              image_url varchar(255) NOT NULL,
                              is_cover boolean DEFAULT false
);

CREATE TABLE venues (
                        id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                        city_id uuid NOT NULL REFERENCES cities(id),
                        name varchar(255) NOT NULL,
                        street_address varchar(255) NOT NULL,
                        phone varchar(50),
                        image_url varchar(255)
);

CREATE TABLE halls (
                       id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       venue_id uuid NOT NULL REFERENCES venues(id),
                       name varchar(50) NOT NULL
);

CREATE TABLE seat_prices (
                             seat_type seat_type PRIMARY KEY,
                             price decimal NOT NULL
);

CREATE TABLE seat_templates (
                                id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                row_num varchar(10) NOT NULL,
                                seat_num varchar(10) NOT NULL,
                                type seat_type DEFAULT 'REGULAR' REFERENCES seat_prices(seat_type),
                                UNIQUE (row_num, seat_num)
);

CREATE TABLE projections (
                             id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             movie_id uuid NOT NULL REFERENCES movies(id),
                             hall_id uuid NOT NULL REFERENCES halls(id),
                             start_time timestamptz NOT NULL,
                             end_time timestamptz NOT NULL,
                             created_at timestamptz DEFAULT now(),
                             UNIQUE (hall_id, start_time)
);

CREATE TABLE bookings (
                          id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id uuid NOT NULL REFERENCES users(id),
                          projection_id uuid NOT NULL REFERENCES projections(id),
                          status booking_status DEFAULT 'HOLD',
                          total_price decimal NOT NULL,
                          expires_at timestamptz NOT NULL,
                          is_reminder_enabled boolean DEFAULT false,
                          created_at timestamptz DEFAULT now()
);

CREATE TABLE booking_seats (
                               id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                               booking_id uuid NOT NULL REFERENCES bookings(id),
                               seat_template_id uuid NOT NULL REFERENCES seat_templates(id),
                               projection_id uuid NOT NULL REFERENCES projections(id),
                               price_snapshot decimal NOT NULL,
                               is_active boolean DEFAULT true,
                               UNIQUE (booking_id, seat_template_id)
);

CREATE UNIQUE INDEX idx_unique_active_seat
    ON booking_seats (projection_id, seat_template_id)
    WHERE is_active = true;

CREATE TABLE user_payment_methods (
                                      id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                      user_id uuid NOT NULL REFERENCES users(id),
                                      stripe_payment_method_id varchar(255) UNIQUE NOT NULL,
                                      card_brand varchar(50) NOT NULL,
                                      last_four varchar(4) NOT NULL,
                                      exp_month int NOT NULL,
                                      exp_year int NOT NULL,
                                      created_at timestamptz DEFAULT now()
);

CREATE TABLE payments (
                          id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                          booking_id uuid NOT NULL REFERENCES bookings(id),
                          stripe_session_id varchar(255) UNIQUE,
                          amount decimal NOT NULL,
                          currency varchar(10) DEFAULT 'BAM',
                          status payment_status DEFAULT 'PENDING',
                          paid_at timestamptz,
                          created_at timestamptz DEFAULT now()
);

CREATE TABLE notifications (
                               id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id uuid NOT NULL REFERENCES users(id),
                               title varchar(255) NOT NULL,
                               message text NOT NULL,
                               is_read boolean DEFAULT false,
                               send_time timestamptz,
                               created_at timestamptz DEFAULT now()
);
