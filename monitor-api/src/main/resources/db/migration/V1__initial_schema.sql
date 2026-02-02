-- V1__initial_schema.sql

-- Sites table
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    frequency_seconds INTEGER NOT NULL DEFAULT 300,
    email_recipients TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sites_enabled ON sites(enabled);
CREATE INDEX idx_sites_created_at ON sites(created_at);

-- Site pages table
CREATE TABLE site_pages (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    path VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(site_id, path)
);

CREATE INDEX idx_site_pages_site_id ON site_pages(site_id);
CREATE INDEX idx_site_pages_enabled ON site_pages(enabled);

-- Rules table
CREATE TABLE rules (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    page_id BIGINT REFERENCES site_pages(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    threshold INTEGER,
    severity VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rules_site_id ON rules(site_id);
CREATE INDEX idx_rules_page_id ON rules(page_id);

-- Runs table
CREATE TABLE runs (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    critical_count INTEGER NOT NULL DEFAULT 0,
    major_count INTEGER NOT NULL DEFAULT 0,
    minor_count INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_runs_site_id ON runs(site_id);
CREATE INDEX idx_runs_started_at ON runs(started_at DESC);
CREATE INDEX idx_runs_status ON runs(status);
CREATE INDEX idx_runs_site_started ON runs(site_id, started_at DESC);

-- Page results table
CREATE TABLE page_results (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    page_id BIGINT NOT NULL REFERENCES site_pages(id) ON DELETE CASCADE,
    final_url VARCHAR(1000),
    ttfb_ms INTEGER,
    dom_ms INTEGER,
    load_ms INTEGER,
    requests_count INTEGER NOT NULL DEFAULT 0,
    total_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_page_results_run_id ON page_results(run_id);
CREATE INDEX idx_page_results_page_id ON page_results(page_id);

-- Failures table
CREATE TABLE failures (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    page_id BIGINT REFERENCES site_pages(id) ON DELETE SET NULL,
    severity VARCHAR(20) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_failures_run_id ON failures(run_id);
CREATE INDEX idx_failures_severity ON failures(severity);
CREATE INDEX idx_failures_type ON failures(type);

-- Request errors table (detailed, retention policy)
CREATE TABLE request_errors (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES runs(id) ON DELETE CASCADE,
    resource_type VARCHAR(50) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    status INTEGER,
    duration_ms INTEGER,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_errors_run_id ON request_errors(run_id);
CREATE INDEX idx_request_errors_created_at ON request_errors(created_at);
CREATE INDEX idx_request_errors_status ON request_errors(status);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for sites
CREATE TRIGGER update_sites_updated_at BEFORE UPDATE ON sites
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE sites IS 'Monitored websites configuration';
COMMENT ON TABLE site_pages IS 'Individual pages to monitor per site';
COMMENT ON TABLE rules IS 'Monitoring rules and thresholds per site/page';
COMMENT ON TABLE runs IS 'Execution runs with aggregated results';
COMMENT ON TABLE page_results IS 'Performance metrics per page per run';
COMMENT ON TABLE failures IS 'Detected issues aggregated by type';
COMMENT ON TABLE request_errors IS 'Detailed HTTP request errors (30 day retention)';
