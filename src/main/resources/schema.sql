CREATE TABLE IF NOT EXISTS payment_plan (
    pp_id                       bigserial     		NOT NULL PRIMARY KEY,
    pp_amount		        	      BIGINT    	 	NOT NULL,
    pp_status      			        TEXT			  	,
    pp_customer_number		      TEXT				NOT NULL,
    pp_mod_date		              TIMESTAMP  	 	NULL,
    pp_mod_user				          TEXT
);