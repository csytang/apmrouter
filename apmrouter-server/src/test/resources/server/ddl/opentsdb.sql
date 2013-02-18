CREATE TABLE IF NOT EXISTS metric (
	id VARCHAR  NOT NULL,
	name VARCHAR  NULL,
	PRIMARY KEY (id)
	);

CREATE TABLE IF NOT EXISTS tag (
	name VARCHAR  NOT NULL,
	value VARCHAR  NOT NULL,
	PRIMARY KEY (name, value)
	);

CREATE TABLE IF NOT EXISTS data_point (
	id INT  NOT NULL,
	metric_id VARCHAR  NULL,
	timestamp TIMESTAMP  NULL,
	long_value BIGINT  NULL,
	double_value DOUBLE  NULL,
	PRIMARY KEY (id),
	CONSTRAINT data_point_metric_id_fkey FOREIGN KEY (metric_id)
		REFERENCES metric (id) 
	);

CREATE TABLE IF NOT EXISTS metric_tag (
	metric_id VARCHAR  NOT NULL,
	tag_name VARCHAR  NOT NULL,
	tag_value VARCHAR  NOT NULL,
	PRIMARY KEY (metric_id, tag_name, tag_value),
	CONSTRAINT metric_tag_metric_id_fkey FOREIGN KEY (metric_id)
		REFERENCES metric (id),
	CONSTRAINT metric_tag_tag_name_fkey FOREIGN KEY (tag_name, tag_value)
		REFERENCES tag (name, value) 
	);

