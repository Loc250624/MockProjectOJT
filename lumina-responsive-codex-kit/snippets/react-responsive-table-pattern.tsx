// Reference only. Rename fields/components and preserve existing business logic.
// The goal is one data source with desktop table + mobile cards, not duplicated fetching.

import type { ReactNode } from "react";

type Column<T> = {
  key: string;
  label: string;
  render: (row: T) => ReactNode;
  mobilePriority?: boolean;
};

type Props<T> = {
  rows: T[];
  columns: Column<T>[];
  getRowKey: (row: T) => string | number;
  renderActions?: (row: T) => ReactNode;
};

export function ResponsiveDataView<T>({
  rows,
  columns,
  getRowKey,
  renderActions,
}: Props<T>) {
  return (
    <>
      <div className="desktopTable" role="region" aria-label="Data table" tabIndex={0}>
        <table>
          <thead>
            <tr>
              {columns.map((column) => <th key={column.key}>{column.label}</th>)}
              {renderActions && <th>Actions</th>}
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={getRowKey(row)}>
                {columns.map((column) => <td key={column.key}>{column.render(row)}</td>)}
                {renderActions && <td>{renderActions(row)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mobileCards">
        {rows.map((row) => (
          <article key={getRowKey(row)} className="dataCard">
            {columns.filter((column) => column.mobilePriority !== false).map((column) => (
              <div key={column.key} className="dataField">
                <span className="dataLabel">{column.label}</span>
                <div className="dataValue">{column.render(row)}</div>
              </div>
            ))}
            {renderActions && <div className="dataActions">{renderActions(row)}</div>}
          </article>
        ))}
      </div>
    </>
  );
}
