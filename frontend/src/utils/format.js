/**
 * Formats a numeric amount as Indonesian Rupiah.
 * Example: 45000 → "Rp 45.000"
 *
 * Uses the 'id-ID' locale so thousands separators use dots (Indonesian convention).
 */
export function formatRupiah(amount) {
  return 'Rp ' + Number(amount).toLocaleString('id-ID', { maximumFractionDigits: 0 })
}
