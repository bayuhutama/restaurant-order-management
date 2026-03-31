export function formatRupiah(amount) {
  return 'Rp ' + Number(amount).toLocaleString('id-ID', { maximumFractionDigits: 0 })
}
