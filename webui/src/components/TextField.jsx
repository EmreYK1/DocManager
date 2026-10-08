export default function TextField({ label, field, multiline = false, autoFocus = false }) {
  const Control = multiline ? 'textarea' : 'input'
  const extraProps = multiline ? { rows: 3 } : { autoFocus }

  return (
    <>
      <label className="field">
        {label}
        <Control value={field.value} onChange={(event) => field.onChange(event.target.value)} {...extraProps} />
      </label>
      {field.visibleError && (
        <p role="alert" className="field-error">
          {field.visibleError}
        </p>
      )}
    </>
  )
}
